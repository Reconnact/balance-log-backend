package de.htwberlin.balancelog.weight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import de.htwberlin.balancelog.user.User;
import de.htwberlin.balancelog.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class WeightEntryServiceTest {

	private static final UUID OWNER_ID = UUID.fromString("5c1d2e3f-4a5b-4c6d-8e7f-9a0b1c2d3e4f");
	private static final UUID ENTRY_ID = UUID.fromString("3f2b8c1e-6d4a-4f7e-9b2a-1c5d8e0f4a71");

	@Mock
	private WeightEntryRepository entries;

	@Mock
	private UserRepository users;

	private WeightEntryService service;

	private User owner;

	@BeforeEach
	void setUp() {
		service = new WeightEntryService(entries, users);
		owner = new User();
		owner.setId(OWNER_ID);
	}

	@Test
	void findAllReturnsTheOwnersEntries() {
		when(entries.findAllByOwnerIdOrderByMeasuredAtAsc(OWNER_ID)).thenReturn(List.of(
				new WeightEntry(ENTRY_ID, owner, Instant.parse("2026-10-01T06:30:00Z"), new BigDecimal("82.4"))));

		assertThat(service.findAll(OWNER_ID)).containsExactly(
				new WeightEntryResponse(ENTRY_ID, Instant.parse("2026-10-01T06:30:00Z"), new BigDecimal("82.4")));
	}

	@Test
	void findByIdReturnsTheOwnersEntry() {
		when(entries.findByIdAndOwnerId(ENTRY_ID, OWNER_ID)).thenReturn(Optional.of(
				new WeightEntry(ENTRY_ID, owner, Instant.parse("2026-10-01T06:30:00Z"), new BigDecimal("82.4"))));

		assertThat(service.findById(ENTRY_ID, OWNER_ID)).isEqualTo(
				new WeightEntryResponse(ENTRY_ID, Instant.parse("2026-10-01T06:30:00Z"), new BigDecimal("82.4")));
	}

	@Test
	void findByIdRejectsAForeignOrMissingEntryWith404() {
		when(entries.findByIdAndOwnerId(ENTRY_ID, OWNER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findById(ENTRY_ID, OWNER_ID))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void updateChangesTheOwnersEntry() {
		when(entries.findByIdAndOwnerId(ENTRY_ID, OWNER_ID)).thenReturn(Optional.of(
				new WeightEntry(ENTRY_ID, owner, Instant.parse("2026-10-01T06:30:00Z"), new BigDecimal("82.4"))));
		when(entries.save(any(WeightEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

		WeightEntryResponse response = service.update(ENTRY_ID, OWNER_ID,
				new WeightEntryUpdateRequest(Instant.parse("2026-10-02T07:00:00Z"), new BigDecimal("81.9")));

		ArgumentCaptor<WeightEntry> saved = ArgumentCaptor.forClass(WeightEntry.class);
		verify(entries).save(saved.capture());
		assertThat(saved.getValue().getOwner()).isSameAs(owner);
		assertThat(response).isEqualTo(
				new WeightEntryResponse(ENTRY_ID, Instant.parse("2026-10-02T07:00:00Z"), new BigDecimal("81.9")));
	}

	@Test
	void updateRejectsAForeignOrMissingEntryWith404() {
		when(entries.findByIdAndOwnerId(ENTRY_ID, OWNER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(ENTRY_ID, OWNER_ID,
				new WeightEntryUpdateRequest(Instant.parse("2026-10-02T07:00:00Z"), new BigDecimal("81.9"))))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
		verify(entries, never()).save(any());
	}

	@Test
	void createSavesTheEntryForTheOwner() {
		when(users.findById(OWNER_ID)).thenReturn(Optional.of(owner));
		when(entries.save(any(WeightEntry.class))).thenAnswer(invocation -> {
			WeightEntry entry = invocation.getArgument(0);
			entry.setId(ENTRY_ID);
			return entry;
		});

		WeightEntryResponse response = service.create(OWNER_ID,
				new WeightEntryRequest(Instant.parse("2026-10-01T06:30:00Z"), new BigDecimal("82.4")));

		ArgumentCaptor<WeightEntry> saved = ArgumentCaptor.forClass(WeightEntry.class);
		verify(entries).save(saved.capture());
		assertThat(saved.getValue().getOwner()).isSameAs(owner);
		assertThat(response).isEqualTo(
				new WeightEntryResponse(ENTRY_ID, Instant.parse("2026-10-01T06:30:00Z"), new BigDecimal("82.4")));
	}

	@Test
	void createUsesNowWhenMeasuredAtIsMissing() {
		when(users.findById(OWNER_ID)).thenReturn(Optional.of(owner));
		when(entries.save(any(WeightEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
		Instant before = Instant.now();

		WeightEntryResponse response = service.create(OWNER_ID, new WeightEntryRequest(null, new BigDecimal("82.4")));

		assertThat(response.measuredAt()).isBetween(before, Instant.now());
	}

	@Test
	void createRejectsAnUnknownOwnerWith401() {
		when(users.findById(OWNER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(OWNER_ID, new WeightEntryRequest(null, new BigDecimal("82.4"))))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
		verify(entries, never()).save(any());
	}

}
