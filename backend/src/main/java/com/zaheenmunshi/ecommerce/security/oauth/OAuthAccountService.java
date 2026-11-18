package com.zaheenmunshi.ecommerce.security.oauth;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zaheenmunshi.ecommerce.auth.service.AuthService;
import com.zaheenmunshi.ecommerce.common.error.ApiException;
import com.zaheenmunshi.ecommerce.user.domain.User;
import com.zaheenmunshi.ecommerce.user.domain.UserIdentity;
import com.zaheenmunshi.ecommerce.user.domain.UserIdentityRepository;
import com.zaheenmunshi.ecommerce.user.domain.UserRepository;

/**
 * Maps an OpenID Connect login (e.g. Google) to a local account: a known identity logs
 * straight in; otherwise the provider-verified email is linked to the existing account
 * or a new one. Unverified emails are refused, so nobody can claim someone else's account.
 */
@Service
public class OAuthAccountService {

	private final UserRepository users;

	private final UserIdentityRepository identities;

	public OAuthAccountService(UserRepository users, UserIdentityRepository identities) {
		this.users = users;
		this.identities = identities;
	}

	@Transactional
	public User findOrCreate(OAuth2AuthenticationToken authentication) {
		if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
			throw ApiException.badRequest("this sign-in provider is not supported");
		}
		String provider = authentication.getAuthorizedClientRegistrationId();
		return identities.findByProviderAndSubject(provider, oidcUser.getSubject())
			.map(UserIdentity::getUser)
			.orElseGet(() -> link(provider, oidcUser));
	}

	private User link(String provider, OidcUser oidcUser) {
		if (oidcUser.getEmail() == null || !Boolean.TRUE.equals(oidcUser.getEmailVerified())) {
			throw ApiException.forbidden("your " + provider + " email address is not verified");
		}
		String email = AuthService.normalizeEmail(oidcUser.getEmail());
		User user = users.findByEmail(email).orElseGet(() -> users.save(User.fromOAuth(displayName(oidcUser), email)));
		identities.save(new UserIdentity(user, provider, oidcUser.getSubject()));
		return user;
	}

	private static String displayName(OidcUser oidcUser) {
		String name = oidcUser.getFullName();
		if (name == null || name.isBlank()) {
			name = oidcUser.getEmail().substring(0, oidcUser.getEmail().indexOf('@'));
		}
		return name.length() > 100 ? name.substring(0, 100) : name;
	}

}
