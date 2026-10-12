package de.htwberlin.balancelog.auth;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MaxBytesValidator implements ConstraintValidator<MaxBytes, String> {

	private int max;

	@Override
	public void initialize(MaxBytes annotation) {
		max = annotation.value();
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return value == null || value.getBytes(StandardCharsets.UTF_8).length <= max;
	}

}
