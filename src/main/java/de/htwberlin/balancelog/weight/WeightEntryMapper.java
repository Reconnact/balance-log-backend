package de.htwberlin.balancelog.weight;

public final class WeightEntryMapper {

	private WeightEntryMapper() {
	}

	public static WeightEntryResponse toResponse(WeightEntry entry) {
		return new WeightEntryResponse(entry.getId(), entry.getMeasuredAt(), entry.getWeightKg());
	}

}
