package de.htwberlin.balancelog.goal;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import de.htwberlin.balancelog.user.User;
import de.htwberlin.balancelog.user.UserRepository;

@Service
public class GoalService {

	private final UserRepository users;

	public GoalService(UserRepository users) {
		this.users = users;
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

	private User user(UUID userId) {
		return users.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}

}
