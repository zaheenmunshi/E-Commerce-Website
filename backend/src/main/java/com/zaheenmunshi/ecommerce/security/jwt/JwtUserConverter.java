package com.zaheenmunshi.ecommerce.security.jwt;

import java.util.List;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.zaheenmunshi.ecommerce.security.AppUserPrincipal;

/**
 * Turns a verified access token into the {@link AppUserPrincipal} controllers receive via
 * {@code @AuthenticationPrincipal}, with ROLE_* authorities from the "roles" claim.
 */
@Component
public class JwtUserConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		List<String> roles = jwt.getClaimAsStringList(AccessTokenService.CLAIM_ROLES);
		AppUserPrincipal principal = AppUserPrincipal.fromToken(UUID.fromString(jwt.getSubject()),
				jwt.getClaimAsString(AccessTokenService.CLAIM_EMAIL), jwt.getClaimAsString(AccessTokenService.CLAIM_NAME),
				roles != null && roles.contains("SELLER"));
		return UsernamePasswordAuthenticationToken.authenticated(principal, jwt, principal.getAuthorities());
	}

}
