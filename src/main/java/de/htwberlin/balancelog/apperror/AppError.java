package de.htwberlin.balancelog.apperror;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum AppError {

	EMAIL_TAKEN(HttpStatus.CONFLICT, "Email already registered."),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Wrong email or password."),
	USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, null),

	WEIGHT_ENTRY_NOT_FOUND(HttpStatus.NOT_FOUND, null),
	NO_WEIGHT_ENTRIES(HttpStatus.NOT_FOUND, "No weight logged."),
	INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "from is after to."),

	GOAL_NOT_FOUND(HttpStatus.NOT_FOUND, "No goal set.");

	private final HttpStatus status;
	private final String detail;

	AppError(HttpStatus status, String detail) {
		this.status = status;
		this.detail = detail;
	}

	public ResponseStatusException exception() {
		return new ResponseStatusException(status, detail);
	}

}
