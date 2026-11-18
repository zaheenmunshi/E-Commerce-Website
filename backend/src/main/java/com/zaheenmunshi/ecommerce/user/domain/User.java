package com.zaheenmunshi.ecommerce.user.domain;

import java.time.Instant;

import com.zaheenmunshi.ecommerce.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "app_users", uniqueConstraints = @UniqueConstraint(name = "uk_app_users_email", columnNames = "email"))
public class User extends BaseEntity {

	@Column(nullable = false, length = 100)
	private String name;

	/** Always stored lower-case; it is the login name. */
	@Column(nullable = false, length = 254)
	private String email;

	/** Null for accounts that only sign in with Google. */
	@Column(name = "password_hash", length = 100)
	private String passwordHash;

	@Column(length = 15)
	private String phone;

	@Column(name = "terms_accepted_at", nullable = false)
	private Instant termsAcceptedAt;

	@Column(name = "marketing_opt_in", nullable = false)
	private boolean marketingOptIn;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role = Role.CUSTOMER;

	protected User() {
	}

	public User(String name, String email, String passwordHash, String phone, boolean marketingOptIn) {
		this.name = name;
		this.email = email;
		this.passwordHash = passwordHash;
		this.phone = phone;
		this.marketingOptIn = marketingOptIn;
		this.termsAcceptedAt = Instant.now();
	}

	/** An account created by "Continue with Google"; it has no password and no phone yet. */
	public static User fromOAuth(String name, String email) {
		return new User(name, email, null, null, false);
	}

	public void promoteToSeller() {
		role = Role.SELLER;
	}

	public boolean isSeller() {
		return role == Role.SELLER;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getPhone() {
		return phone;
	}

	public Instant getTermsAcceptedAt() {
		return termsAcceptedAt;
	}

	public boolean isMarketingOptIn() {
		return marketingOptIn;
	}

	public Role getRole() {
		return role;
	}

}
