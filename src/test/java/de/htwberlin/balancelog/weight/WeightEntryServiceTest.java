package de.htwberlin.balancelog.weight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

	private static final UUID USER_ID = UUID.fromString("5c1d2e3f-4a5b-4c6d-8e7f-9a0b1c2d3e4f");
	private static final UUID ENTRY_ID = UUID.fromString("3f2b8c1e-6d4a-4f7e-9b2a-1c5d8e0f4a71");

	@Mock
	private WeightEntryRepository weightEntryRepository;

	@Mock
	private UserRepository userRepository;

	private WeightEntryService weightEntryService;

	private User owner;

	@BeforeEach
	void setUp() {
		weightEntryService = new WeightEntryService(weightEntryRepository, userRepository);
		owner = new User();
		owner.setId(USER_ID);
	}

	@Test
	void findAllReturnsTheOwnersEntries() {
		Instant from = Instant.parse("2026-09-01T00:00:00Z");
		Instant to = Instant.parse("2026-10-31T00:00:00Z");
		when(weightEntryRepository.findAllInPeriod(USER_ID, from, to)).thenReturn(List.of(
				new WeightEntry(ENTRY_ID, owner, Instant.parse("2026-10-01T06:30:00Z"), new BigDecimal("82.4"))));

		assertThat(weightEntryService.listWeightEntries(USER_ID, from, to)).containsExactly(
				new WeightEntryResponse(ENTRY_ID, Instant.parse("2026-10-01T06:30:00Z"), new BigDecimal("82.4")));
	}

	@Test
	void findAllRejectsFromAfterToWith400() {
		assertThatThrownBy(() -> weightEntryService.listWeightEntries(USER_ID, Instant.parse("2026-10-31T00:00:00Z"),
				Instant.parse("2026-09-01T00:00:00Z")))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
		verifyNoInteractions(weightEntryRepository);
	}

	@Test
	void findByIdReturnsTheOwnersEntry() {
		when(weightEntryRepository.findByIdAndOwnerId(ENTRY_ID, USER_ID)).thenReturn(Optional.of(
				new WeightEntry(ENTRY_ID, owner, Instant.parse("2026-10-01T06:30:00Z"), new BigDecimal("82.4"))));

		assertThat(weightEntryService.getWeightEntry(USER_ID, ENTRY_ID)).isEqualTo(
				new WeightEntryResponse(ENTRY_ID, Instant.parse("2026-10-01T06:30:00Z"), new BigDecimal("82.4")));
	}

	@Test
	void findByIdRejectsAForeignOrMissingEntryWith404() {
		when(weightEntryRepository.findByIdAndOwnerId(ENTRY_ID, USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> weightEntryService.getWeightEntry(USER_ID, ENTRY_ID))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void updateChangesTheOwnersEntry() {
		when(weightEntryRepository.findByIdAndOwnerId(ENTRY_ID, USER_ID)).thenReturn(Optional.of(
				new WeightEntry(ENTRY_ID, owner, Instant.parse("2026-10-01T06:30:00Z"), new BigDecimal("82.4"))));
		when(weightEntryRepository.save(any(WeightEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

		WeightEntryResponse response = weightEntryService.updateWeightEntry(USER_ID, ENTRY_ID,
				new UpdateWeightEntryRequest(Instant.parse("2026-10-02T07:00:00Z"), new BigDecimal("81.9")));

		ArgumentCaptor<WeightEntry> saved = ArgumentCaptor.forClass(WeightEntry.class);
		verify(weightEntryRepository).save(saved.capture());
		assertThat(saved.getValue().getOwner()).isSameAs(owner);
		assertThat(response).isEqualTo(
				new WeightEntryResponse(ENTRY_ID, Instant.parse("2026-10-02T07:00:00Z"), new BigDecimal("81.9")));
	}

	@Test
	void updateRejectsAForeignOrMissingEntryWith404() {
		when(weightEntryRepository.findByIdAndOwnerId(ENTRY_ID, USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> weightEntryService.updateWeightEntry(USER_ID, ENTRY_ID,
				new UpdateWeightEntryRequest(Instant.parse("2026-10-02T07:00:00Z"), new BigDecimal("81.9"))))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
		verify(weightEntryRepository, never()).save(any());
	}

	@Test
	void deleteRemovesTheOwnersEntry() {
		WeightEntry entry = new WeightEntry(ENTRY_ID, owner, Instant.parse("2026-10-01T06:30:00Z"), new BigDecimal("82.4"));
		when(weightEntryRepository.findByIdAndOwnerId(ENTRY_ID, USER_ID)).thenReturn(Optional.of(entry));

		weightEntryService.deleteWeightEntry(USER_ID, ENTRY_ID);

		verify(weightEntryRepository).delete(entry);
	}

	@Test
	void deleteRejectsAForeignOrMissingEntryWith404() {
		when(weightEntryRepository.findByIdAndOwnerId(ENTRY_ID, USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> weightEntryService.deleteWeightEntry(USER_ID, ENTRY_ID))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
		verify(weightEntryRepository, never()).delete(any());
	}

	@Test
	void createSavesTheEntryForTheOwner() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
		when(weightEntryRepository.save(any(WeightEntry.class))).thenAnswer(invocation -> {
			WeightEntry entry = invocation.getArgument(0);
			entry.setId(ENTRY_ID);
			return entry;
		});

		WeightEntryResponse response = weightEntryService.createWeightEntry(USER_ID,
				new CreateWeightEntryRequest(Instant.parse("2026-10-01T06:30:00Z"), new BigDecimal("82.4")));

		ArgumentCaptor<WeightEntry> saved = ArgumentCaptor.forClass(WeightEntry.class);
		verify(weightEntryRepository).save(saved.capture());
		assertThat(saved.getValue().getOwner()).isSameAs(owner);
		assertThat(response).isEqualTo(
				new WeightEntryResponse(ENTRY_ID, Instant.parse("2026-10-01T06:30:00Z"), new BigDecimal("82.4")));
	}

	@Test
	void createUsesNowWhenMeasuredAtIsMissing() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
		when(weightEntryRepository.save(any(WeightEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
		Instant before = Instant.now();

		WeightEntryResponse response = weightEntryService.createWeightEntry(USER_ID, new CreateWeightEntryRequest(null, new BigDecimal("82.4")));

		assertThat(response.measuredAt()).isBetween(before, Instant.now());
	}

	@Test
	void createRejectsAnUnknownOwnerWith401() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> weightEntryService.createWeightEntry(USER_ID, new CreateWeightEntryRequest(null, new BigDecimal("82.4"))))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
		verify(weightEntryRepository, never()).save(any());
	}

}
