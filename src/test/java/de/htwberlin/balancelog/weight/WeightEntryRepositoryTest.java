package de.htwberlin.balancelog.weight;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import de.htwberlin.balancelog.user.User;

@DataJpaTest
class WeightEntryRepositoryTest {

	@Autowired
	private WeightEntryRepository repository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void findAllByOwnerReturnsOnlyTheOwnersEntriesOldestFirst() {
		User anna = user("anna@example.com");
		User ben = user("ben@example.com");
		WeightEntry later = entry(anna, "2026-10-03T06:45:00Z", "82.1");
		WeightEntry earlier = entry(anna, "2026-10-01T06:30:00Z", "82.4");
		entry(ben, "2026-10-02T07:00:00Z", "75.0");
		entityManager.flush();
		entityManager.clear();

		assertThat(repository.findAllByOwnerIdOrderByMeasuredAtAsc(anna.getId()))
				.extracting(WeightEntry::getId)
				.containsExactly(earlier.getId(), later.getId());
	}

	private User user(String email) {
		User user = new User();
		user.setEmail(email);
		user.setPasswordHash("hash");
		return entityManager.persist(user);
	}

	private WeightEntry entry(User owner, String measuredAt, String weightKg) {
		return entityManager.persist(new WeightEntry(null, owner, Instant.parse(measuredAt), new BigDecimal(weightKg)));
	}

}
