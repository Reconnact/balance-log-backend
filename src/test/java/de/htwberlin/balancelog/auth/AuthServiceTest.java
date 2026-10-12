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
	private UserRepository users;

	@Mock
	private TokenService tokenService;

	private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

	private AuthService service;

	@BeforeEach
	void setUp() {
		service = new AuthService(users, passwordEncoder, tokenService);
	}

	@Test
	void registerLowercasesEmailAndHashesPassword() {
		when(users.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			user.setId(ID);
			return user;
		});

		UserResponse response = service.register(new RegisterRequest("Anna@Example.com", "12345678"));

		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(users).save(saved.capture());
		assertThat(saved.getValue().getEmail()).isEqualTo("anna@example.com");
		assertThat(saved.getValue().getPasswordHash()).startsWith("{bcrypt}");
		assertThat(passwordEncoder.matches("12345678", saved.getValue().getPasswordHash())).isTrue();
		assertThat(response).isEqualTo(new UserResponse(ID, "anna@example.com"));
	}

	@Test
	void registerRejectsTakenEmailWith409() {
		when(users.existsByEmail("anna@example.com")).thenReturn(true);

		assertThatThrownBy(() -> service.register(new RegisterRequest("Anna@example.com", "12345678")))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
		verify(users, never()).save(any());
	}

	@Test
	void loginReturnsTokenForRightPassword() {
		User user = user("12345678");
		TokenResponse token = new TokenResponse("token", Instant.parse("2026-10-19T10:00:00Z"));
		when(users.findByEmail("anna@example.com")).thenReturn(Optional.of(user));
		when(tokenService.create(user)).thenReturn(token);

		assertThat(service.login(new LoginRequest("Anna@example.com", "12345678"))).isEqualTo(token);
	}

	@Test
	void loginRejectsWrongPasswordWith401() {
		when(users.findByEmail("anna@example.com")).thenReturn(Optional.of(user("12345678")));

		assertThatThrownBy(() -> service.login(new LoginRequest("anna@example.com", "87654321")))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
	}

	@Test
	void loginRejectsUnknownEmailWith401() {
		when(users.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.login(new LoginRequest("nobody@example.com", "12345678")))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
	}

	@Test
	void meReturnsUser() {
		when(users.findById(ID)).thenReturn(Optional.of(user("12345678")));

		assertThat(service.me(ID)).isEqualTo(new UserResponse(ID, "anna@example.com"));
	}

	@Test
	void meRejectsUnknownUserWith401() {
		when(users.findById(ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.me(ID))
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
