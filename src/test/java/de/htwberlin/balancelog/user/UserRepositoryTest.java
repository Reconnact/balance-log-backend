package de.htwberlin.balancelog.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import de.htwberlin.balancelog.TestcontainersConfiguration;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

	@Autowired
	private UserRepository repository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void findByEmailReturnsSavedUser() {
		User saved = repository.save(user("anna@example.com"));
		entityManager.flush();
		entityManager.clear();

		assertThat(repository.findByEmail("anna@example.com"))
				.hasValueSatisfying(found -> {
					assertThat(found.getId()).isEqualTo(saved.getId());
					assertThat(found.getPasswordHash()).isEqualTo("hash");
					assertThat(found.getCreatedAt()).isNotNull();
				});
	}

	@Test
	void findByEmailReturnsEmptyForUnknownEmail() {
		assertThat(repository.findByEmail("nobody@example.com")).isEmpty();
	}

	@Test
	void emailIsUnique() {
		repository.save(user("anna@example.com"));
		repository.save(user("anna@example.com"));

		assertThatThrownBy(entityManager::flush).isInstanceOf(ConstraintViolationException.class);
	}

	private static User user(String email) {
		User user = new User();
		user.setEmail(email);
		user.setPasswordHash("hash");
		return user;
	}

}
