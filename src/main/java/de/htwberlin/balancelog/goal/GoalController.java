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

	private final GoalService service;

	public GoalController(GoalService service) {
		this.service = service;
	}

	@GetMapping("/goal")
	public GoalResponse get(@AuthenticationPrincipal Jwt jwt) {
		return service.get(UUID.fromString(jwt.getSubject()));
	}

	@PutMapping("/goal")
	public GoalResponse set(@Valid @RequestBody GoalRequest request, @AuthenticationPrincipal Jwt jwt) {
		return service.set(UUID.fromString(jwt.getSubject()), request);
	}

}
