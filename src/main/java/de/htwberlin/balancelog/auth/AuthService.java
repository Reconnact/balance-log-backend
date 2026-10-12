package de.htwberlin.balancelog.auth;

import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import de.htwberlin.balancelog.user.User;
import de.htwberlin.balancelog.user.UserRepository;

@Service
public class AuthService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokenService) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
	}

	public UserResponse register(RegisterRequest request) {
		String email = request.email().toLowerCase(Locale.ROOT);
		if (users.existsByEmail(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered.");
		}
		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		User saved = users.save(user);
		return new UserResponse(saved.getId(), saved.getEmail());
	}

	public TokenResponse login(LoginRequest request) {
		return users.findByEmail(request.email().toLowerCase(Locale.ROOT))
				.filter(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
				.map(tokenService::create)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password."));
	}

	public UserResponse me(UUID id) {
		return users.findById(id)
				.map(user -> new UserResponse(user.getId(), user.getEmail()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}

}
