package de.htwberlin.balancelog.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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
import de.htwberlin.balancelog.weight.WeightEntry;
import de.htwberlin.balancelog.weight.WeightEntryRepository;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

	private static final UUID USER_ID = UUID.fromString("5c1d2e3f-4a5b-4c6d-8e7f-9a0b1c2d3e4f");
	private static final Instant NOW = Instant.parse("2026-10-29T08:00:00Z");
	private static final Instant WINDOW_START = NOW.minus(Duration.ofDays(28));

	@Mock
	private UserRepository userRepository;

	@Mock
	private WeightEntryRepository weightEntryRepository;

	private GoalService goalService;

	private User user;

	@BeforeEach
	void setUp() {
		goalService = new GoalService(userRepository, weightEntryRepository, Clock.fixed(NOW, ZoneOffset.UTC));
		user = new User();
		user.setId(USER_ID);
	}

	@Test
	void getReturnsTheGoal() {
		user.setGoalKg(new BigDecimal("75.0"));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

		assertThat(goalService.getGoal(USER_ID)).isEqualTo(new GoalResponse(new BigDecimal("75.0")));
	}

	@Test
	void getRejectsAMissingGoalWith404() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> goalService.getGoal(USER_ID))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void getRejectsAnUnknownUserWith401() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> goalService.getGoal(USER_ID))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
	}

	@Test
	void setStoresTheGoal() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(userRepository.save(user)).thenReturn(user);

		GoalResponse response = goalService.setGoal(USER_ID, new SetGoalRequest(new BigDecimal("75.0")));

		assertThat(user.getGoalKg()).isEqualTo(new BigDecimal("75.0"));
		verify(userRepository).save(user);
		assertThat(response).isEqualTo(new GoalResponse(new BigDecimal("75.0")));
	}

	@Test
	void progressReturnsLatestTargetAndKgToGo() {
		user.setGoalKg(new BigDecimal("75.0"));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		WeightEntry latest = entry("2026-10-28T06:30:00Z", "82.0");
		when(weightEntryRepository.findFirstByOwnerIdOrderByMeasuredAtDesc(USER_ID)).thenReturn(Optional.of(latest));
		when(weightEntryRepository.findAllInPeriod(USER_ID, WINDOW_START, null)).thenReturn(List.of(latest));

		ProgressResponse progress = goalService.getProgress(USER_ID);

		assertThat(progress.latestKg()).isEqualTo(new BigDecimal("82.0"));
		assertThat(progress.targetKg()).isEqualTo(new BigDecimal("75.0"));
		assertThat(progress.kgToGo()).isEqualTo(new BigDecimal("-7.0"));
	}

	@Test
	void progressKgPerWeekIsTheSlopeOverTheLast28Days() {
		user.setGoalKg(new BigDecimal("75.0"));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		List<WeightEntry> window = List.of(
				entry("2026-10-07T07:00:00Z", "82.0"),
				entry("2026-10-14T07:00:00Z", "81.5"),
				entry("2026-10-21T07:00:00Z", "81.0"),
				entry("2026-10-28T07:00:00Z", "80.5"));
		when(weightEntryRepository.findFirstByOwnerIdOrderByMeasuredAtDesc(USER_ID)).thenReturn(Optional.of(window.get(3)));
		when(weightEntryRepository.findAllInPeriod(USER_ID, WINDOW_START, null)).thenReturn(window);

		assertThat(goalService.getProgress(USER_ID).kgPerWeek()).isEqualTo(new BigDecimal("-0.5"));
	}

	@Test
	void progressKgPerWeekIsNullUnderTwoEntries() {
		user.setGoalKg(new BigDecimal("75.0"));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		WeightEntry latest = entry("2026-10-28T06:30:00Z", "82.0");
		when(weightEntryRepository.findFirstByOwnerIdOrderByMeasuredAtDesc(USER_ID)).thenReturn(Optional.of(latest));
		when(weightEntryRepository.findAllInPeriod(USER_ID, WINDOW_START, null)).thenReturn(List.of(latest));

		assertThat(goalService.getProgress(USER_ID).kgPerWeek()).isNull();
	}

	@Test
	void progressKgPerWeekIsNullUnderSevenDaysBetweenFirstAndLast() {
		user.setGoalKg(new BigDecimal("75.0"));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		List<WeightEntry> window = List.of(
				entry("2026-10-22T07:00:00Z", "82.0"),
				entry("2026-10-28T07:00:00Z", "81.0"));
		when(weightEntryRepository.findFirstByOwnerIdOrderByMeasuredAtDesc(USER_ID)).thenReturn(Optional.of(window.get(1)));
		when(weightEntryRepository.findAllInPeriod(USER_ID, WINDOW_START, null)).thenReturn(window);

		assertThat(goalService.getProgress(USER_ID).kgPerWeek()).isNull();
	}

	@Test
	void progressRejectsAMissingGoalWith404() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> goalService.getProgress(USER_ID))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void progressRejectsNoEntryWith404() {
		user.setGoalKg(new BigDecimal("75.0"));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(weightEntryRepository.findFirstByOwnerIdOrderByMeasuredAtDesc(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> goalService.getProgress(USER_ID))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void progressRejectsAnUnknownUserWith401() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> goalService.getProgress(USER_ID))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
	}

	@Test
	void setRejectsAnUnknownUserWith401() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> goalService.setGoal(USER_ID, new SetGoalRequest(new BigDecimal("75.0"))))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
		verify(userRepository, never()).save(any());
	}

	private WeightEntry entry(String measuredAt, String weightKg) {
		return new WeightEntry(UUID.randomUUID(), user, Instant.parse(measuredAt), new BigDecimal(weightKg));
	}

}
