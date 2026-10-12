package de.htwberlin.balancelog.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank(message = "Enter a valid email address.")
		@Email(message = "Enter a valid email address.")
		@Size(max = 254, message = "Enter a valid email address.")
		String email,

		@NotNull(message = "8 to 72 characters.")
		@Size(min = 8, max = 72, message = "8 to 72 characters.")
		@MaxBytes(value = 72, message = "8 to 72 characters.")
		String password) {
}
