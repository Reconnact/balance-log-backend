package de.htwberlin.balancelog.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import de.htwberlin.balancelog.user.User;
import de.htwberlin.balancelog.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final UUID ID = UUID.fromString("5c1d2e3f-4a5b-4c6d-8e7f-9a0b1c2d3e4f");

	@Mock
	private UserRepository userRepository;

	@Mock
	private TokenService tokenService;

	private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(userRepository, passwordEncoder, tokenService);
	}

	@Test
	void registerLowercasesEmailAndHashesPassword() {
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			user.setId(ID);
			return user;
		});

		UserResponse response = authService.register(new RegisterRequest("Anna@Example.com", "12345678"));

		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(saved.capture());
		assertThat(saved.getValue().getEmail()).isEqualTo("anna@example.com");
		assertThat(saved.getValue().getPasswordHash()).startsWith("{bcrypt}");
		assertThat(passwordEncoder.matches("12345678", saved.getValue().getPasswordHash())).isTrue();
		assertThat(response).isEqualTo(new UserResponse(ID, "anna@example.com"));
	}

	@Test
	void registerRejectsTakenEmailWith409() {
		when(userRepository.existsByEmail("anna@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(new RegisterRequest("Anna@example.com", "12345678")))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
		verify(userRepository, never()).save(any());
	}

	@Test
	void loginReturnsTokenForRightPassword() {
		User user = user("12345678");
		TokenResponse token = new TokenResponse("token", Instant.parse("2026-10-19T10:00:00Z"));
		when(userRepository.findByEmail("anna@example.com")).thenReturn(Optional.of(user));
		when(tokenService.create(user)).thenReturn(token);

		assertThat(authService.login(new LoginRequest("Anna@example.com", "12345678"))).isEqualTo(token);
	}

	@Test
	void loginRejectsWrongPasswordWith401() {
		when(userRepository.findByEmail("anna@example.com")).thenReturn(Optional.of(user("12345678")));

		assertThatThrownBy(() -> authService.login(new LoginRequest("anna@example.com", "87654321")))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
	}

	@Test
	void loginRejectsUnknownEmailWith401() {
		when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "12345678")))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
	}

	@Test
	void profileReturnsUser() {
		when(userRepository.findById(ID)).thenReturn(Optional.of(user("12345678")));

		assertThat(authService.getProfile(ID)).isEqualTo(new UserResponse(ID, "anna@example.com"));
	}

	@Test
	void profileRejectsUnknownUserWith401() {
		when(userRepository.findById(ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.getProfile(ID))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
	}

	private User user(String password) {
		User user = new User();
		user.setId(ID);
		user.setEmail("anna@example.com");
		user.setPasswordHash(passwordEncoder.encode(password));
		return user;
	}

}
