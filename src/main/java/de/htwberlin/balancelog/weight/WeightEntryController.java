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

	private final WeightEntryService service;

	public WeightEntryController(WeightEntryService service) {
		this.service = service;
	}

	@GetMapping
	public List<WeightEntryResponse> findAll(@RequestParam(required = false) Instant from,
			@RequestParam(required = false) Instant to, @AuthenticationPrincipal Jwt jwt) {
		return service.findAll(ownerId(jwt), from, to);
	}

	@GetMapping("/{id}")
	public WeightEntryResponse findById(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		return service.findById(id, ownerId(jwt));
	}

	@PostMapping
	public ResponseEntity<WeightEntryResponse> create(@Valid @RequestBody WeightEntryRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		WeightEntryResponse created = service.create(ownerId(jwt), request);
		return ResponseEntity.created(URI.create("/weight-entries/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	public WeightEntryResponse update(@PathVariable UUID id, @Valid @RequestBody WeightEntryUpdateRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return service.update(id, ownerId(jwt), request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		service.delete(id, ownerId(jwt));
	}

	private static UUID ownerId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

}
