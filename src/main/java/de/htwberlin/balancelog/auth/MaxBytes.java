package de.htwberlin.balancelog.auth;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Limits a string's UTF-8 length. {@code @Size} counts characters, but BCrypt rejects passwords over 72 bytes.
 */
@Target(FIELD)
@Retention(RUNTIME)
@Constraint(validatedBy = MaxBytesValidator.class)
public @interface MaxBytes {

	int value();

	String message();

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
