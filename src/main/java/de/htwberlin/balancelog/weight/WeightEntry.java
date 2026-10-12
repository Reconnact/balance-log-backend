package de.htwberlin.balancelog.weight;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import de.htwberlin.balancelog.user.User;

@Entity
@Table(indexes = @Index(columnList = "owner_id, measured_at"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WeightEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(nullable = false)
	private User owner;

	private Instant measuredAt;

	@Column(precision = 4, scale = 1)
	private BigDecimal weightKg;

}
