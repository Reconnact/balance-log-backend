package de.htwberlin.balancelog.goal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.server.ResponseStatusException;

import de.htwberlin.balancelog.auth.SecurityConfig;

@WebMvcTest(GoalController.class)
@Import(SecurityConfig.class)
class GoalControllerTest {

	private static final UUID USER_ID = UUID.fromString("5c1d2e3f-4a5b-4c6d-8e7f-9a0b1c2d3e4f");
	private static final String TARGET_MESSAGE = "Between 20 and 400 kg, one decimal.";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GoalService goalService;

	@Test
	void getGoalReturnsTheGoal() throws Exception {
		when(goalService.getGoal(USER_ID)).thenReturn(new GoalResponse(new BigDecimal("75.0")));

		mockMvc.perform(get("/goal").with(user()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.targetKg").value(75.0));
	}

	@Test
	void getGoalReturns404WithoutAGoal() throws Exception {
		when(goalService.getGoal(USER_ID)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

		mockMvc.perform(get("/goal").with(user()))
				.andExpect(status().isNotFound());
	}

	@Test
	void getGoalReturns401WithoutToken() throws Exception {
		mockMvc.perform(get("/goal"))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(goalService);
	}

	@Test
	void getProgressReturnsTheProgress() throws Exception {
		when(goalService.getProgress(USER_ID)).thenReturn(new ProgressResponse(new BigDecimal("82.0"),
				new BigDecimal("75.0"), new BigDecimal("-7.0"), null));

		mockMvc.perform(get("/progress").with(user()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.latestKg").value(82.0))
				.andExpect(jsonPath("$.targetKg").value(75.0))
				.andExpect(jsonPath("$.kgToGo").value(-7.0))
				.andExpect(jsonPath("$.kgPerWeek").isEmpty());
	}

	@Test
	void getProgressReturns404WithoutGoalOrEntry() throws Exception {
		when(goalService.getProgress(USER_ID)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

		mockMvc.perform(get("/progress").with(user()))
				.andExpect(status().isNotFound());
	}

	@Test
	void getProgressReturns401WithoutToken() throws Exception {
		mockMvc.perform(get("/progress"))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(goalService);
	}

	@Test
	void putGoalReturns200WithTheGoal() throws Exception {
		when(goalService.setGoal(eq(USER_ID), any())).thenReturn(new GoalResponse(new BigDecimal("75.0")));

		mockMvc.perform(put("/goal").with(user()).contentType(MediaType.APPLICATION_JSON).content(body("75.0")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.targetKg").value(75.0));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidRequests")
	void putGoalReturns400PerRule(String rule, String content) throws Exception {
		mockMvc.perform(put("/goal").with(user()).contentType(MediaType.APPLICATION_JSON).content(content))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.targetKg").value(TARGET_MESSAGE));
		verifyNoInteractions(goalService);
	}

	static Stream<Arguments> invalidRequests() {
		return Stream.of(
				Arguments.of("target missing", "{}"),
				Arguments.of("target under 20", body("19.9")),
				Arguments.of("target over 400", body("400.1")),
				Arguments.of("target with two decimals", body("75.05")));
	}

	@Test
	void putGoalReturns401WithoutToken() throws Exception {
		mockMvc.perform(put("/goal").contentType(MediaType.APPLICATION_JSON).content(body("75.0")))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(goalService);
	}

	private static String body(String targetKg) {
		return "{\"targetKg\":" + targetKg + "}";
	}

	private static RequestPostProcessor user() {
		return jwt().jwt(token -> token.subject(USER_ID.toString()));
	}

}
