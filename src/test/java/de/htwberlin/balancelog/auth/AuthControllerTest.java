package de.htwberlin.balancelog.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

	private static final UUID ID = UUID.fromString("5c1d2e3f-4a5b-4c6d-8e7f-9a0b1c2d3e4f");
	private static final String EMAIL_MESSAGE = "Enter a valid email address.";
	private static final String PASSWORD_MESSAGE = "8 to 72 characters.";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@Test
	void registerReturns201() throws Exception {
		when(authService.register(any())).thenReturn(new UserResponse(ID, "anna@example.com"));

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(body("anna@example.com", "12345678")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(ID.toString()))
				.andExpect(jsonPath("$.email").value("anna@example.com"));
	}

	@Test
	void registerReturns409ForTakenEmail() throws Exception {
		when(authService.register(any())).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT));

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(body("anna@example.com", "12345678")))
				.andExpect(status().isConflict());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidRequests")
	void registerReturns400PerRule(String rule, String content, String field, String message) throws Exception {
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(content))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors." + field).value(message));
		verifyNoInteractions(authService);
	}

	static Stream<Arguments> invalidRequests() {
		return Stream.of(
				Arguments.of("email blank", body("", "12345678"), "email", EMAIL_MESSAGE),
				Arguments.of("email not an address", body("anna", "12345678"), "email", EMAIL_MESSAGE),
				Arguments.of("email over 254", body("a".repeat(243) + "@example.com", "12345678"), "email",
						EMAIL_MESSAGE),
				Arguments.of("password missing", "{\"email\":\"anna@example.com\"}", "password", PASSWORD_MESSAGE),
				Arguments.of("password under 8", body("anna@example.com", "1234567"), "password", PASSWORD_MESSAGE),
				Arguments.of("password over 72 characters", body("anna@example.com", "a".repeat(73)), "password",
						PASSWORD_MESSAGE),
				Arguments.of("password over 72 bytes", body("anna@example.com", "ä".repeat(37)), "password",
						PASSWORD_MESSAGE));
	}

	@Test
	void loginReturns200WithToken() throws Exception {
		when(authService.login(any())).thenReturn(new TokenResponse("token", Instant.parse("2026-10-19T10:00:00Z")));

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(body("anna@example.com", "12345678")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("token"))
				.andExpect(jsonPath("$.expiresAt").value("2026-10-19T10:00:00Z"));
	}

	@Test
	void loginReturns400ForInvalidBody() throws Exception {
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body("", "12345678")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.email").value(EMAIL_MESSAGE));
	}

	@Test
	void loginReturns401ForWrongPassword() throws Exception {
		when(authService.login(any())).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(body("anna@example.com", "87654321")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void meReturns401WithoutToken() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void meReturns200WithToken() throws Exception {
		when(authService.me(ID)).thenReturn(new UserResponse(ID, "anna@example.com"));

		mockMvc.perform(get("/api/auth/me").with(jwt().jwt(token -> token.subject(ID.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ID.toString()))
				.andExpect(jsonPath("$.email").value("anna@example.com"));
	}

	private static String body(String email, String password) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
	}

}
