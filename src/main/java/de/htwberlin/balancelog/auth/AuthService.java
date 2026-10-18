package de.htwberlin.balancelog.auth;

import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import de.htwberlin.balancelog.apperror.AppError;
import de.htwberlin.balancelog.user.User;
import de.htwberlin.balancelog.user.UserRepository;

@Slf4j
@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
	}

	public UserResponse register(RegisterRequest request) {
		String email = request.email().toLowerCase(Locale.ROOT);
		if (userRepository.existsByEmail(email)) {
			throw AppError.EMAIL_TAKEN.exception();
		}

		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		User created = userRepository.save(user);

		log.info("user registered user_id={}", created.getId());

		return UserMapper.toResponse(created);
	}

	public TokenResponse login(LoginRequest request) {
		String email = request.email().toLowerCase(Locale.ROOT);
		User user = userRepository.findByEmail(email).orElse(null);
		if (user == null) {
			log.warn("login failed: unknown email email={}", email);
			throw AppError.INVALID_CREDENTIALS.exception();
		}

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			log.warn("login failed: wrong password user_id={}", user.getId());
			throw AppError.INVALID_CREDENTIALS.exception();
		}

		log.info("login successful user_id={}", user.getId());

		return tokenService.create(user);
	}

	public UserResponse getProfile(UUID userId) {
		return userRepository.findById(userId)
				.map(UserMapper::toResponse)
				.orElseThrow(AppError.USER_NOT_FOUND::exception);
	}

}
