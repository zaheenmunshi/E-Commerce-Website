package com.zaheenmunshi.ecommerce.security.refresh;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zaheenmunshi.ecommerce.config.AppProperties;
import com.zaheenmunshi.ecommerce.user.domain.User;

/**
 * Refresh-token rotation with reuse detection (OAuth 2.0 Security BCP): each refresh
 * swaps the token for a new one; presenting a token that was already swapped means it
 * leaked, so the whole login is revoked. A short grace window keeps two browser tabs
 * refreshing at the same moment from tripping that alarm.
 */
@Service
public class RefreshTokenService {

	private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

	private static final Duration CONCURRENT_REFRESH_GRACE = Duration.ofSeconds(30);

	private static final int TOKEN_BYTES = 32;

	private final SecureRandom random = new SecureRandom();

	private final RefreshTokenRepository tokens;

	private final AppProperties.Security settings;

	private final Clock clock;

	public RefreshTokenService(RefreshTokenRepository tokens, AppProperties properties, Clock clock) {
		this.tokens = tokens;
		this.settings = properties.security();
		this.clock = clock;
	}

	/** Starts a new login and returns the raw token for the cookie. */
	@Transactional
	public String issue(User user) {
		Instant now = clock.instant();
		return save(user, UUID.randomUUID(), now, now.plus(settings.sessionMaxAge()));
	}

	@Transactional
	public Rotation rotate(String rawToken) {
		RefreshToken current = tokens.findByTokenHash(hash(rawToken)).orElse(null);
		Instant now = clock.instant();
		if (current == null || current.isExpired(now)) {
			return new Rotation.Invalid();
		}
		if (current.isRevoked()) {
			if (current.isRotatedAfter(now.minus(CONCURRENT_REFRESH_GRACE))) {
				return new Rotation.Retry();
			}
			int revoked = tokens.revokeFamily(current.getFamilyId(), now);
			log.warn("Refresh token reuse detected for user {}; revoked {} token(s) of that login",
					current.getUser().getId(), revoked);
			return new Rotation.Invalid();
		}
		current.markRotated(now);
		String next = save(current.getUser(), current.getFamilyId(), now, current.getFamilyExpiresAt());
		return new Rotation.Rotated(next, current.getUser());
	}

	/** Logout: ends the login the token belongs to. Unknown tokens are ignored. */
	@Transactional
	public void revoke(String rawToken) {
		tokens.findByTokenHash(hash(rawToken)).ifPresent(token -> tokens.revokeFamily(token.getFamilyId(), clock.instant()));
	}

	@Scheduled(cron = "0 30 3 * * *")
	@Transactional
	public void deleteExpired() {
		int deleted = tokens.deleteExpiredBefore(clock.instant().minus(Duration.ofDays(1)));
		log.info("Deleted {} expired refresh token(s)", deleted);
	}

	private String save(User user, UUID familyId, Instant now, Instant familyExpiresAt) {
		byte[] bytes = new byte[TOKEN_BYTES];
		random.nextBytes(bytes);
		String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		Instant expiresAt = now.plus(settings.refreshTokenTtl());
		tokens.save(new RefreshToken(user, familyId, hash(raw),
				expiresAt.isBefore(familyExpiresAt) ? expiresAt : familyExpiresAt, familyExpiresAt));
		return raw;
	}

	private static String hash(String rawToken) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is always available", ex);
		}
	}

	/** Outcome of {@link #rotate}; a sealed type so callers must handle every case. */
	public sealed interface Rotation {

		/** The new refresh token for the cookie and the user to issue an access token for. */
		record Rotated(String token, User user) implements Rotation {
		}

		/** Unknown, expired or revoked: the user has to log in again. */
		record Invalid() implements Rotation {
		}

		/** Another tab refreshed this token a moment ago: retry with the new cookie. */
		record Retry() implements Rotation {
		}

	}

}
