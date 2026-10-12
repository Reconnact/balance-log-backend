package de.htwberlin.balancelog.goal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import de.htwberlin.balancelog.user.User;
import de.htwberlin.balancelog.user.UserRepository;
import de.htwberlin.balancelog.weight.WeightEntry;
import de.htwberlin.balancelog.weight.WeightEntryRepository;

@Service
public class GoalService {

	private static final Duration RATE_WINDOW = Duration.ofDays(28);
	private static final Duration MIN_RATE_SPAN = Duration.ofDays(7);

	private final UserRepository users;
	private final WeightEntryRepository entries;
	private final Clock clock;

	public GoalService(UserRepository users, WeightEntryRepository entries, Clock clock) {
		this.users = users;
		this.entries = entries;
		this.clock = clock;
	}

	public GoalResponse get(UUID userId) {
		User user = user(userId);
		if (user.getGoalKg() == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No goal set.");
		}
		return new GoalResponse(user.getGoalKg());
	}

	public GoalResponse set(UUID userId, GoalRequest request) {
		User user = user(userId);
		user.setGoalKg(request.targetKg());
		return new GoalResponse(users.save(user).getGoalKg());
	}

	public ProgressResponse progress(UUID userId) {
		User user = user(userId);
		if (user.getGoalKg() == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No goal set.");
		}
		WeightEntry latest = entries.findFirstByOwnerIdOrderByMeasuredAtDesc(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No weight logged."));
		List<WeightEntry> window = entries.findAllInPeriod(userId, clock.instant().minus(RATE_WINDOW), null);
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

	private User user(UUID userId) {
		return users.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}

}
