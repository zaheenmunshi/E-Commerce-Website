package com.zaheenmunshi.ecommerce.security.jwt;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.zaheenmunshi.ecommerce.config.AppProperties;

/**
 * Signs and verifies the API's access tokens (HMAC-SHA256). The decoder pins the
 * algorithm and checks issuer, audience, expiry and not-before on every request.
 */
@Configuration
public class JwtConfig {

	private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

	private static final int MIN_KEY_BYTES = 32;

	@Bean
	SecretKey jwtSigningKey(AppProperties properties) {
		String secret = properties.security().jwt().secret();
		byte[] key;
		if (secret == null || secret.isBlank()) {
			key = new byte[MIN_KEY_BYTES];
			new SecureRandom().nextBytes(key);
			log.warn("JWT_SECRET is not set: using a random key, so logins end when the app restarts");
		}
		else {
			key = Base64.getDecoder().decode(secret.trim());
			if (key.length < MIN_KEY_BYTES) {
				throw new IllegalStateException("JWT_SECRET must be a base64 key of at least 256 bits");
			}
		}
		return new SecretKeySpec(key, "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
		return NimbusJwtEncoder.withSecretKey(jwtSigningKey).build();
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey jwtSigningKey, AppProperties properties, Clock clock) {
		AppProperties.Jwt jwt = properties.security().jwt();
		var timestamps = new JwtTimestampValidator(Duration.ofSeconds(30));
		timestamps.setClock(clock);
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamps, new JwtIssuerValidator(jwt.issuer()),
				new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
						audience -> audience != null && audience.contains(jwt.audience()))));
		return decoder;
	}

}
