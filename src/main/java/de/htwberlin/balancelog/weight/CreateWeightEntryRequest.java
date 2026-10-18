package de.htwberlin.balancelog.weight;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

public record CreateWeightEntryRequest(
		@PastOrPresent(message = "Can't be in the future.")
		Instant measuredAt,

		@NotNull(message = "Between 20 and 400 kg, one decimal.")
		@DecimalMin(value = "20.0", message = "Between 20 and 400 kg, one decimal.")
		@DecimalMax(value = "400.0", message = "Between 20 and 400 kg, one decimal.")
		@Digits(integer = 3, fraction = 1, message = "Between 20 and 400 kg, one decimal.")
		BigDecimal weightKg) {
}
