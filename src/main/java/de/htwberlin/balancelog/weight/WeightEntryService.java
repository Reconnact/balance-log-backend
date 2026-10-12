package de.htwberlin.balancelog.weight;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class WeightEntryService {

	private final WeightEntryRepository entries;

	public WeightEntryService(WeightEntryRepository entries) {
		this.entries = entries;
	}

	public List<WeightEntryResponse> findAll(UUID ownerId) {
		return entries.findAllByOwnerIdOrderByMeasuredAtAsc(ownerId).stream()
				.map(WeightEntryService::toResponse)
				.toList();
	}

	private static WeightEntryResponse toResponse(WeightEntry entry) {
		return new WeightEntryResponse(entry.getId(), entry.getMeasuredAt(), entry.getWeightKg());
	}

}
