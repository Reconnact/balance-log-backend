package de.htwberlin.balancelog.goal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import de.htwberlin.balancelog.apperror.AppError;
import de.htwberlin.balancelog.user.User;
import de.htwberlin.balancelog.user.UserRepository;
import de.htwberlin.balancelog.weight.WeightEntry;
import de.htwberlin.balancelog.weight.WeightEntryRepository;

@Slf4j
@Service
public class GoalService {

	private static final Duration RATE_WINDOW = Duration.ofDays(28);
	private static final Duration MIN_RATE_SPAN = Duration.ofDays(7);

	private final UserRepository userRepository;
	private final WeightEntryRepository weightEntryRepository;
	private final Clock clock;

	public GoalService(UserRepository userRepository, WeightEntryRepository weightEntryRepository, Clock clock) {
		this.userRepository = userRepository;
		this.weightEntryRepository = weightEntryRepository;
		this.clock = clock;
	}

	public GoalResponse getGoal(UUID userId) {
		User user = getUser(userId);
		if (user.getGoalKg() == null) {
			throw AppError.GOAL_NOT_FOUND.exception();
		}

		return new GoalResponse(user.getGoalKg());
	}

	public GoalResponse setGoal(UUID userId, SetGoalRequest request) {
		User user = getUser(userId);

		user.setGoalKg(request.targetKg());
		User updated = userRepository.save(user);

		log.info("goal set user_id={}", userId);

		return new GoalResponse(updated.getGoalKg());
	}

	public ProgressResponse getProgress(UUID userId) {
		User user = getUser(userId);
		if (user.getGoalKg() == null) {
			throw AppError.GOAL_NOT_FOUND.exception();
		}

		WeightEntry latest = weightEntryRepository.findFirstByOwnerIdOrderByMeasuredAtDesc(userId)
				.orElseThrow(AppError.NO_WEIGHT_ENTRIES::exception);
		List<WeightEntry> window = weightEntryRepository.findAllInPeriod(userId, clock.instant().minus(RATE_WINDOW), null);

		return new ProgressResponse(latest.getWeightKg(), user.getGoalKg(),
				user.getGoalKg().subtract(latest.getWeightKg()), kgPerWeek(window));
	}

	private static BigDecimal kgPerWeek(List<WeightEntry> entries) {
		if (entries.size() < 2) {
			return null;
		}
		Instant first = entries.getFirst().getMeasuredAt();
		if (Duration.between(first, entries.getLast().getMeasuredAt()).compareTo(MIN_RATE_SPAN) < 0) {
			return null;
		}

		double[] days = entries.stream()
				.mapToDouble(entry -> Duration.between(first, entry.getMeasuredAt()).toSeconds() / 86_400.0)
				.toArray();
		double[] kgs = entries.stream().mapToDouble(entry -> entry.getWeightKg().doubleValue()).toArray();
		double meanDay = Arrays.stream(days).average().orElseThrow();
		double meanKg = Arrays.stream(kgs).average().orElseThrow();

		double covariance = 0;
		double variance = 0;
		for (int i = 0; i < days.length; i++) {
			covariance += (days[i] - meanDay) * (kgs[i] - meanKg);
			variance += (days[i] - meanDay) * (days[i] - meanDay);
		}
		return BigDecimal.valueOf(covariance / variance * 7).setScale(1, RoundingMode.HALF_UP);
	}

	private User getUser(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(AppError.USER_NOT_FOUND::exception);
	}

}
