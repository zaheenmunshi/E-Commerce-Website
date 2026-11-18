package com.zaheenmunshi.ecommerce.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.zaheenmunshi.ecommerce.user.domain.Role;
import com.zaheenmunshi.ecommerce.user.domain.User;

/**
 * The authenticated user: built from the database during password login, and from the
 * access token's claims on every API call afterwards (no database hit per request).
 */
public final class AppUserPrincipal implements UserDetails, CredentialsContainer {

	private final UUID id;

	private final String email;

	private final String name;

	private final Role role;

	private String passwordHash;

	private AppUserPrincipal(UUID id, String email, String name, Role role, String passwordHash) {
		this.id = id;
		this.email = email;
		this.name = name;
		this.role = role;
		this.passwordHash = passwordHash;
	}

	public static AppUserPrincipal from(User user) {
		return new AppUserPrincipal(user.getId(), user.getEmail(), user.getName(), user.getRole(),
				user.getPasswordHash());
	}

	public static AppUserPrincipal fromToken(UUID id, String email, String name, boolean seller) {
		return new AppUserPrincipal(id, email, name, seller ? Role.SELLER : Role.CUSTOMER, null);
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public boolean isSeller() {
		return role == Role.SELLER;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// sellers keep the customer role so they can still shop
		return isSeller()
				? List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"), new SimpleGrantedAuthority("ROLE_SELLER"))
				: List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public void eraseCredentials() {
		passwordHash = null;
	}

}
