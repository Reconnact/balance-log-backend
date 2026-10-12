package de.htwberlin.balancelog.weight;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

@WebMvcTest(WeightEntryController.class)
@Import(SecurityConfig.class)
class WeightEntryControllerTest {

	private static final UUID OWNER_ID = UUID.fromString("5c1d2e3f-4a5b-4c6d-8e7f-9a0b1c2d3e4f");
	private static final UUID ENTRY_ID = UUID.fromString("3f2b8c1e-6d4a-4f7e-9b2a-1c5d8e0f4a71");
	private static final String WEIGHT_MESSAGE = "Between 20 and 400 kg, one decimal.";
	private static final String MEASURED_AT_MESSAGE = "Can't be in the future.";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private WeightEntryService service;

	@Test
	void getWeightEntriesReturns401WithoutToken() throws Exception {
		mockMvc.perform(get("/weight-entries"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void getWeightEntriesReturnsTheOwnersEntries() throws Exception {
		when(service.findAll(OWNER_ID)).thenReturn(List.of(
				new WeightEntryResponse(UUID.fromString("3f2b8c1e-6d4a-4f7e-9b2a-1c5d8e0f4a71"),
						Instant.parse("2026-10-01T06:30:00Z"), new BigDecimal("82.4")),
				new WeightEntryResponse(UUID.fromString("a9e47d20-1b6c-4c3f-8e5d-7f2a9b0c6d13"),
						Instant.parse("2026-10-03T06:45:00Z"), new BigDecimal("82.1"))));

		mockMvc.perform(get("/weight-entries").with(owner()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").value("3f2b8c1e-6d4a-4f7e-9b2a-1c5d8e0f4a71"))
				.andExpect(jsonPath("$[0].measuredAt").value("2026-10-01T06:30:00Z"))
				.andExpect(jsonPath("$[0].weightKg").value(82.4))
				.andExpect(jsonPath("$[0].owner").doesNotExist())
				.andExpect(jsonPath("$[1].id").value("a9e47d20-1b6c-4c3f-8e5d-7f2a9b0c6d13"));
	}

	@Test
	void getWeightEntryReturnsTheEntry() throws Exception {
		when(service.findById(ENTRY_ID, OWNER_ID)).thenReturn(
				new WeightEntryResponse(ENTRY_ID, Instant.parse("2026-10-01T06:30:00Z"), new BigDecimal("82.4")));

		mockMvc.perform(get("/weight-entries/{id}", ENTRY_ID).with(owner()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ENTRY_ID.toString()))
				.andExpect(jsonPath("$.weightKg").value(82.4));
	}

	@Test
	void getWeightEntryReturns404ForAForeignId() throws Exception {
		when(service.findById(ENTRY_ID, OWNER_ID)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

		mockMvc.perform(get("/weight-entries/{id}", ENTRY_ID).with(owner()))
				.andExpect(status().isNotFound());
	}

	@Test
	void getWeightEntryReturns400ForAnIdThatIsNoUuid() throws Exception {
		mockMvc.perform(get("/weight-entries/{id}", "42").with(owner()))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(service);
	}

	@Test
	void getWeightEntryReturns401WithoutToken() throws Exception {
		mockMvc.perform(get("/weight-entries/{id}", ENTRY_ID))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void postWeightEntryReturns201WithLocation() throws Exception {
		when(service.create(eq(OWNER_ID), any())).thenReturn(
				new WeightEntryResponse(ENTRY_ID, Instant.parse("2026-10-01T06:30:00Z"), new BigDecimal("82.4")));

		mockMvc.perform(post("/weight-entries").with(owner()).contentType(MediaType.APPLICATION_JSON)
				.content(body("2026-10-01T06:30:00Z", "82.4")))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/weight-entries/" + ENTRY_ID))
				.andExpect(jsonPath("$.id").value(ENTRY_ID.toString()))
				.andExpect(jsonPath("$.measuredAt").value("2026-10-01T06:30:00Z"))
				.andExpect(jsonPath("$.weightKg").value(82.4));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidRequests")
	void postWeightEntryReturns400PerRule(String rule, String content, String field, String message)
			throws Exception {
		mockMvc.perform(post("/weight-entries").with(owner()).contentType(MediaType.APPLICATION_JSON)
				.content(content))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors." + field).value(message));
		verifyNoInteractions(service);
	}

	static Stream<Arguments> invalidRequests() {
		return Stream.of(
				Arguments.of("weight missing", "{\"measuredAt\":\"2026-10-01T06:30:00Z\"}", "weightKg",
						WEIGHT_MESSAGE),
				Arguments.of("weight under 20", body("2026-10-01T06:30:00Z", "19.9"), "weightKg", WEIGHT_MESSAGE),
				Arguments.of("weight over 400", body("2026-10-01T06:30:00Z", "400.1"), "weightKg", WEIGHT_MESSAGE),
				Arguments.of("weight with two decimals", body("2026-10-01T06:30:00Z", "82.45"), "weightKg",
						WEIGHT_MESSAGE),
				Arguments.of("measuredAt in the future", body("2999-01-01T00:00:00Z", "82.4"), "measuredAt",
						MEASURED_AT_MESSAGE));
	}

	@Test
	void postWeightEntryReturns401WithoutToken() throws Exception {
		mockMvc.perform(post("/weight-entries").contentType(MediaType.APPLICATION_JSON)
				.content(body("2026-10-01T06:30:00Z", "82.4")))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(service);
	}

	private static String body(String measuredAt, String weightKg) {
		return "{\"measuredAt\":\"" + measuredAt + "\",\"weightKg\":" + weightKg + "}";
	}

	private static RequestPostProcessor owner() {
		return jwt().jwt(token -> token.subject(OWNER_ID.toString()));
	}

}
