package de.htwberlin.balancelog.weight;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

class WeightEntryServiceTest {

	private final WeightEntryService service = new WeightEntryService();

	@Test
	void findAllReturnsFiveEntriesOldestFirst() {
		List<WeightEntry> entries = service.findAll();

		assertThat(entries)
				.hasSize(5)
				.isSortedAccordingTo(Comparator.comparing(WeightEntry::getMeasuredAt));
	}

}
