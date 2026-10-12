package de.htwberlin.balancelog.weight;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface WeightEntryRepository extends CrudRepository<WeightEntry, UUID> {

	// Without the cast Postgres can't type a null parameter in "is null" and fails the query.
	@Query("""
			select e from WeightEntry e
			where e.owner.id = :ownerId
				and (cast(:from as Instant) is null or e.measuredAt >= :from)
				and (cast(:to as Instant) is null or e.measuredAt <= :to)
			order by e.measuredAt""")
	List<WeightEntry> findAllInPeriod(UUID ownerId, Instant from, Instant to);

	Optional<WeightEntry> findByIdAndOwnerId(UUID id, UUID ownerId);

}
