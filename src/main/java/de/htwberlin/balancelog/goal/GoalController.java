package de.htwberlin.balancelog.goal;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class GoalController {

	private final GoalService goalService;

	public GoalController(GoalService goalService) {
		this.goalService = goalService;
	}

	@GetMapping("/goal")
	public GoalResponse getGoal(@AuthenticationPrincipal Jwt jwt) {
		return goalService.getGoal(UUID.fromString(jwt.getSubject()));
	}

	@GetMapping("/progress")
	public ProgressResponse getProgress(@AuthenticationPrincipal Jwt jwt) {
		return goalService.getProgress(UUID.fromString(jwt.getSubject()));
	}

	@PutMapping("/goal")
	public GoalResponse setGoal(@Valid @RequestBody SetGoalRequest request, @AuthenticationPrincipal Jwt jwt) {
		return goalService.setGoal(UUID.fromString(jwt.getSubject()), request);
	}

}
