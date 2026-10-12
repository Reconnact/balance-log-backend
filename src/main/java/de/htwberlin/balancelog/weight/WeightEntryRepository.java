package de.htwberlin.balancelog.weight;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

public interface WeightEntryRepository extends CrudRepository<WeightEntry, UUID> {

	List<WeightEntry> findAllByOwnerIdOrderByMeasuredAtAsc(UUID ownerId);

}
