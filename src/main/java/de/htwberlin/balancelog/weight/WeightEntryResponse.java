package de.htwberlin.balancelog.weight;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WeightEntryResponse(UUID id, Instant measuredAt, BigDecimal weightKg) {
}
