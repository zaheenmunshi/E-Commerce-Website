package com.zaheenmunshi.ecommerce.auth.dto;

import java.time.Instant;

import com.zaheenmunshi.ecommerce.security.jwt.AccessTokenService.AccessToken;
import com.zaheenmunshi.ecommerce.user.domain.User;

/** OAuth 2.0 style token response; the refresh token is in an HttpOnly cookie, never here. */
public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, UserResponse user) {

	public static AuthResponse of(AccessToken token, User user) {
		return new AuthResponse(token.value(), "Bearer", token.expiresAt(), UserResponse.from(user));
	}

}
