package com.zaheenmunshi.ecommerce.user.domain;

import com.zaheenmunshi.ecommerce.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Links an account to an external OAuth 2.0 / OpenID Connect login, e.g. ("google", sub). */
@Entity
@Table(name = "user_identities",
		uniqueConstraints = @UniqueConstraint(name = "uk_user_identities_provider_subject",
				columnNames = { "provider", "subject" }))
public class UserIdentity extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 40)
	private String provider;

	@Column(nullable = false, length = 255)
	private String subject;

	protected UserIdentity() {
	}

	public UserIdentity(User user, String provider, String subject) {
		this.user = user;
		this.provider = provider;
		this.subject = subject;
	}

	public User getUser() {
		return user;
	}

}
