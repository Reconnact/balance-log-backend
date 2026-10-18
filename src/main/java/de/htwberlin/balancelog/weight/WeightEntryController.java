package de.htwberlin.balancelog.weight;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/weight-entries")
public class WeightEntryController {

	private final WeightEntryService weightEntryService;

	public WeightEntryController(WeightEntryService weightEntryService) {
		this.weightEntryService = weightEntryService;
	}

	@GetMapping
	public List<WeightEntryResponse> listWeightEntries(@RequestParam(required = false) Instant from,
			@RequestParam(required = false) Instant to, @AuthenticationPrincipal Jwt jwt) {
		return weightEntryService.listWeightEntries(userId(jwt), from, to);
	}

	@GetMapping("/{id}")
	public WeightEntryResponse getWeightEntry(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		return weightEntryService.getWeightEntry(userId(jwt), id);
	}

	@PostMapping
	public ResponseEntity<WeightEntryResponse> createWeightEntry(@Valid @RequestBody CreateWeightEntryRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		WeightEntryResponse created = weightEntryService.createWeightEntry(userId(jwt), request);
		return ResponseEntity.created(URI.create("/weight-entries/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	public WeightEntryResponse updateWeightEntry(@PathVariable UUID id,
			@Valid @RequestBody UpdateWeightEntryRequest request, @AuthenticationPrincipal Jwt jwt) {
		return weightEntryService.updateWeightEntry(userId(jwt), id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteWeightEntry(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		weightEntryService.deleteWeightEntry(userId(jwt), id);
	}

	private static UUID userId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

}
