package de.htwberlin.balancelog.auth;

import de.htwberlin.balancelog.user.User;

public final class UserMapper {

	private UserMapper() {
	}

	public static UserResponse toResponse(User user) {
		return new UserResponse(user.getId(), user.getEmail());
	}

}
