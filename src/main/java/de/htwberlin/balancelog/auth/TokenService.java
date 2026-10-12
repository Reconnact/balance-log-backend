package de.htwberlin.balancelog.auth;

import java.time.Duration;
import java.time.Instant;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import de.htwberlin.balancelog.user.User;

@Service
public class TokenService {

	private static final Duration VALIDITY = Duration.ofDays(7);

	private final JwtEncoder encoder;

	public TokenService(JwtEncoder encoder) {
		this.encoder = encoder;
	}

	public TokenResponse create(User user) {
		Instant expiresAt = Instant.now().plus(VALIDITY);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(user.getId().toString())
				.expiresAt(expiresAt)
				.build();
		String token = encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
		return new TokenResponse(token, expiresAt);
	}

}
