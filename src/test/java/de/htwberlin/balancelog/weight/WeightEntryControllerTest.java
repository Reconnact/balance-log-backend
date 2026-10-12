package de.htwberlin.balancelog.weight;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import de.htwberlin.balancelog.auth.SecurityConfig;

@WebMvcTest(WeightEntryController.class)
@Import(SecurityConfig.class)
class WeightEntryControllerTest {

	private static final UUID OWNER_ID = UUID.fromString("5c1d2e3f-4a5b-4c6d-8e7f-9a0b1c2d3e4f");

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

	private static RequestPostProcessor owner() {
		return jwt().jwt(token -> token.subject(OWNER_ID.toString()));
	}

}
