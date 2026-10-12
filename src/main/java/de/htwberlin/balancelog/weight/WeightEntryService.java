package de.htwberlin.balancelog.weight;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class WeightEntryService {

	private final List<WeightEntry> entries = List.of(
			entry("3f2b8c1e-6d4a-4f7e-9b2a-1c5d8e0f4a71", "2026-10-01T06:30:00Z", "82.4"),
			entry("a9e47d20-1b6c-4c3f-8e5d-7f2a9b0c6d13", "2026-10-03T06:45:00Z", "82.1"),
			entry("71bf64ee-62e4-408e-b5df-5afbd1b76247", "2026-10-05T07:10:00Z", "81.9"),
			entry("e076581b-33dc-462a-bbf1-865500cd1667", "2026-10-08T06:20:00Z", "82.0"),
			entry("00e94b85-63c4-4b65-ac03-3552ca86ec7c", "2026-10-10T06:55:00Z", "81.6"));

	public List<WeightEntry> findAll() {
		return entries;
	}

	private static WeightEntry entry(String id, String measuredAt, String weightKg) {
		return new WeightEntry(UUID.fromString(id), Instant.parse(measuredAt), new BigDecimal(weightKg));
	}

}
