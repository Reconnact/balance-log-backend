package de.htwberlin.balancelog.auth;

import java.time.Instant;

public record TokenResponse(String token, Instant expiresAt) {
}
