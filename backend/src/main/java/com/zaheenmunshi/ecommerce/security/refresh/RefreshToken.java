package com.zaheenmunshi.ecommerce.security.refresh;

import java.time.Instant;
import java.util.UUID;

import com.zaheenmunshi.ecommerce.common.domain.BaseEntity;
import com.zaheenmunshi.ecommerce.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One refresh token. Only its SHA-256 hash is stored. Every token of one login shares a
 * {@code familyId}, so presenting an already-rotated token can revoke the whole login.
 */
@Entity
@Table(name = "refresh_tokens",
		uniqueConstraints = @UniqueConstraint(name = "uk_refresh_tokens_hash", columnNames = "token_hash"),
		indexes = { @Index(name = "ix_refresh_tokens_family", columnList = "family_id"),
				@Index(name = "ix_refresh_tokens_expires", columnList = "expires_at") })
public class RefreshToken extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "family_id", nullable = false)
	private UUID familyId;

	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "family_expires_at", nullable = false)
	private Instant familyExpiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	/** True when revoked because a newer token replaced it (normal use), not by logout or theft. */
	@Column(nullable = false)
	private boolean rotated;

	protected RefreshToken() {
	}

	RefreshToken(User user, UUID familyId, String tokenHash, Instant expiresAt, Instant familyExpiresAt) {
		this.user = user;
		this.familyId = familyId;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
		this.familyExpiresAt = familyExpiresAt;
	}

	void markRotated(Instant now) {
		revokedAt = now;
		rotated = true;
	}

	boolean isRevoked() {
		return revokedAt != null;
	}

	boolean isRotatedAfter(Instant instant) {
		return rotated && revokedAt.isAfter(instant);
	}

	boolean isExpired(Instant now) {
		return !expiresAt.isAfter(now);
	}

	User getUser() {
		return user;
	}

	UUID getFamilyId() {
		return familyId;
	}

	Instant getFamilyExpiresAt() {
		return familyExpiresAt;
	}

}
