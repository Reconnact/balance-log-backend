package de.htwberlin.balancelog.weight;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import de.htwberlin.balancelog.apperror.AppError;
import de.htwberlin.balancelog.user.User;
import de.htwberlin.balancelog.user.UserRepository;

@Slf4j
@Service
public class WeightEntryService {

	private final WeightEntryRepository weightEntryRepository;
	private final UserRepository userRepository;

	public WeightEntryService(WeightEntryRepository weightEntryRepository, UserRepository userRepository) {
		this.weightEntryRepository = weightEntryRepository;
		this.userRepository = userRepository;
	}

	public List<WeightEntryResponse> listWeightEntries(UUID userId, Instant from, Instant to) {
		if (from != null && to != null && from.isAfter(to)) {
			throw AppError.INVALID_DATE_RANGE.exception();
		}

		return weightEntryRepository.findAllInPeriod(userId, from, to).stream()
				.map(WeightEntryMapper::toResponse)
				.toList();
	}

	public WeightEntryResponse getWeightEntry(UUID userId, UUID id) {
		return weightEntryRepository.findByIdAndOwnerId(id, userId)
				.map(WeightEntryMapper::toResponse)
				.orElseThrow(AppError.WEIGHT_ENTRY_NOT_FOUND::exception);
	}

	public WeightEntryResponse createWeightEntry(UUID userId, CreateWeightEntryRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(AppError.USER_NOT_FOUND::exception);

		Instant measuredAt = Instant.now();
		if (request.measuredAt() != null) {
			measuredAt = request.measuredAt();
		}

		WeightEntry entry = weightEntryRepository.save(new WeightEntry(null, user, measuredAt, request.weightKg()));

		log.info("weight entry created user_id={} entry_id={}", userId, entry.getId());

		return WeightEntryMapper.toResponse(entry);
	}

	public WeightEntryResponse updateWeightEntry(UUID userId, UUID id, UpdateWeightEntryRequest request) {
		WeightEntry entry = weightEntryRepository.findByIdAndOwnerId(id, userId)
				.orElseThrow(AppError.WEIGHT_ENTRY_NOT_FOUND::exception);

		entry.setMeasuredAt(request.measuredAt());
		entry.setWeightKg(request.weightKg());
		WeightEntry updated = weightEntryRepository.save(entry);

		log.info("weight entry updated user_id={} entry_id={}", userId, id);

		return WeightEntryMapper.toResponse(updated);
	}

	public void deleteWeightEntry(UUID userId, UUID id) {
		WeightEntry entry = weightEntryRepository.findByIdAndOwnerId(id, userId)
				.orElseThrow(AppError.WEIGHT_ENTRY_NOT_FOUND::exception);

		weightEntryRepository.delete(entry);

		log.info("weight entry deleted user_id={} entry_id={}", userId, id);
	}

}
