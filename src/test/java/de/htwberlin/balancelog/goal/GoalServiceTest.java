package de.htwberlin.balancelog.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import de.htwberlin.balancelog.user.User;
import de.htwberlin.balancelog.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

	private static final UUID USER_ID = UUID.fromString("5c1d2e3f-4a5b-4c6d-8e7f-9a0b1c2d3e4f");

	@Mock
	private UserRepository users;

	private GoalService service;

	private User user;

	@BeforeEach
	void setUp() {
		service = new GoalService(users);
		user = new User();
		user.setId(USER_ID);
	}

	@Test
	void getReturnsTheGoal() {
		user.setGoalKg(new BigDecimal("75.0"));
		when(users.findById(USER_ID)).thenReturn(Optional.of(user));

		assertThat(service.get(USER_ID)).isEqualTo(new GoalResponse(new BigDecimal("75.0")));
	}

	@Test
	void getRejectsAMissingGoalWith404() {
		when(users.findById(USER_ID)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> service.get(USER_ID))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void getRejectsAnUnknownUserWith401() {
		when(users.findById(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(USER_ID))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
	}

	@Test
	void setStoresTheGoal() {
		when(users.findById(USER_ID)).thenReturn(Optional.of(user));
		when(users.save(user)).thenReturn(user);

		GoalResponse response = service.set(USER_ID, new GoalRequest(new BigDecimal("75.0")));

		assertThat(user.getGoalKg()).isEqualTo(new BigDecimal("75.0"));
		verify(users).save(user);
		assertThat(response).isEqualTo(new GoalResponse(new BigDecimal("75.0")));
	}

	@Test
	void setRejectsAnUnknownUserWith401() {
		when(users.findById(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.set(USER_ID, new GoalRequest(new BigDecimal("75.0"))))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
		verify(users, never()).save(any());
	}

}
