package de.htwberlin.balancelog.goal;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record GoalRequest(
		@NotNull(message = "Between 20 and 400 kg, one decimal.")
		@DecimalMin(value = "20.0", message = "Between 20 and 400 kg, one decimal.")
		@DecimalMax(value = "400.0", message = "Between 20 and 400 kg, one decimal.")
		@Digits(integer = 3, fraction = 1, message = "Between 20 and 400 kg, one decimal.")
		BigDecimal targetKg) {
}
