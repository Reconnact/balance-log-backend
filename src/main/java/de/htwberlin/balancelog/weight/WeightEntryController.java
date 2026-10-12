package de.htwberlin.balancelog.weight;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/weight-entries")
public class WeightEntryController {

	private final WeightEntryService service;

	public WeightEntryController(WeightEntryService service) {
		this.service = service;
	}

	@GetMapping
	public List<WeightEntryResponse> findAll(@AuthenticationPrincipal Jwt jwt) {
		return service.findAll(UUID.fromString(jwt.getSubject()));
	}

}
