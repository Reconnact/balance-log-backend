package de.htwberlin.balancelog.weight;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import de.htwberlin.balancelog.user.User;
import de.htwberlin.balancelog.user.UserRepository;

@Service
public class WeightEntryService {

	private final WeightEntryRepository entries;
	private final UserRepository users;

	public WeightEntryService(WeightEntryRepository entries, UserRepository users) {
		this.entries = entries;
		this.users = users;
	}

	public List<WeightEntryResponse> findAll(UUID ownerId, Instant from, Instant to) {
		if (from != null && to != null && from.isAfter(to)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from is after to.");
		}
		return entries.findAllInPeriod(ownerId, from, to).stream()
				.map(WeightEntryService::toResponse)
				.toList();
	}

	public WeightEntryResponse findById(UUID id, UUID ownerId) {
		return entries.findByIdAndOwnerId(id, ownerId)
				.map(WeightEntryService::toResponse)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	public WeightEntryResponse create(UUID ownerId, WeightEntryRequest request) {
		User owner = users.findById(ownerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		Instant measuredAt = request.measuredAt() != null ? request.measuredAt() : Instant.now();
		return toResponse(entries.save(new WeightEntry(null, owner, measuredAt, request.weightKg())));
	}

	public WeightEntryResponse update(UUID id, UUID ownerId, WeightEntryUpdateRequest request) {
		WeightEntry entry = entries.findByIdAndOwnerId(id, ownerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		entry.setMeasuredAt(request.measuredAt());
		entry.setWeightKg(request.weightKg());
		return toResponse(entries.save(entry));
	}

	public void delete(UUID id, UUID ownerId) {
		WeightEntry entry = entries.findByIdAndOwnerId(id, ownerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		entries.delete(entry);
	}

	private static WeightEntryResponse toResponse(WeightEntry entry) {
		return new WeightEntryResponse(entry.getId(), entry.getMeasuredAt(), entry.getWeightKg());
	}

}
