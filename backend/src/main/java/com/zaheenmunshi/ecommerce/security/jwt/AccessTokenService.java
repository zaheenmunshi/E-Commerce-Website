package com.zaheenmunshi.ecommerce.security.jwt;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.zaheenmunshi.ecommerce.config.AppProperties;
import com.zaheenmunshi.ecommerce.user.domain.User;

/** Issues short-lived JWT access tokens; see {@link JwtUserConverter} for the reverse. */
@Service
public class AccessTokenService {

	static final String CLAIM_EMAIL = "email";

	static final String CLAIM_NAME = "name";

	static final String CLAIM_ROLES = "roles";

	private final JwtEncoder encoder;

	private final AppProperties.Jwt settings;

	private final Clock clock;

	public AccessTokenService(JwtEncoder encoder, AppProperties properties, Clock clock) {
		this.encoder = encoder;
		this.settings = properties.security().jwt();
		this.clock = clock;
	}

	public AccessToken issue(User user) {
		Instant now = clock.instant();
		Instant expiresAt = now.plus(settings.accessTokenTtl());
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(settings.issuer())
			.audience(List.of(settings.audience()))
			.subject(user.getId().toString())
			.id(UUID.randomUUID().toString())
			.issuedAt(now)
			.notBefore(now)
			.expiresAt(expiresAt)
			.claim(CLAIM_EMAIL, user.getEmail())
			.claim(CLAIM_NAME, user.getName())
			.claim(CLAIM_ROLES, user.isSeller() ? List.of("CUSTOMER", "SELLER") : List.of("CUSTOMER"))
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
		return new AccessToken(encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(), expiresAt);
	}

	public record AccessToken(String value, Instant expiresAt) {
	}

}
