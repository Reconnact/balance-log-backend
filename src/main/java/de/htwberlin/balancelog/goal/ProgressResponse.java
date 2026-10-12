package de.htwberlin.balancelog.goal;

import java.math.BigDecimal;

public record ProgressResponse(BigDecimal latestKg, BigDecimal targetKg, BigDecimal kgToGo, BigDecimal kgPerWeek) {
}
