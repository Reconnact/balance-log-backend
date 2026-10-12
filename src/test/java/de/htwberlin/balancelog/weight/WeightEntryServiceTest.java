package de.htwberlin.balancelog.weight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.htwberlin.balancelog.user.User;

@ExtendWith(MockitoExtension.class)
class WeightEntryServiceTest {

	private static final UUID OWNER_ID = UUID.fromString("5c1d2e3f-4a5b-4c6d-8e7f-9a0b1c2d3e4f");
	private static final UUID ENTRY_ID = UUID.fromString("3f2b8c1e-6d4a-4f7e-9b2a-1c5d8e0f4a71");

	@Mock
	private WeightEntryRepository entries;

	private WeightEntryService service;

	@BeforeEach
	void setUp() {
		service = new WeightEntryService(entries);
	}

	@Test
	void findAllReturnsTheOwnersEntries() {
		User owner = new User();
		owner.setId(OWNER_ID);
		when(entries.findAllByOwnerIdOrderByMeasuredAtAsc(OWNER_ID)).thenReturn(List.of(
				new WeightEntry(ENTRY_ID, owner, Instant.parse("2026-10-01T06:30:00Z"), new BigDecimal("82.4"))));

		assertThat(service.findAll(OWNER_ID)).containsExactly(
				new WeightEntryResponse(ENTRY_ID, Instant.parse("2026-10-01T06:30:00Z"), new BigDecimal("82.4")));
	}

}
