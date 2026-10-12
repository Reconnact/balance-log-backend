package de.htwberlin.balancelog.auth;

import java.util.UUID;

public record UserResponse(UUID id, String email) {
}
