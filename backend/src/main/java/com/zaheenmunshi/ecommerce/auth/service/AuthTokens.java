package com.zaheenmunshi.ecommerce.auth.service;

import org.springframework.stereotype.Component;

import com.zaheenmunshi.ecommerce.auth.dto.AuthResponse;
import com.zaheenmunshi.ecommerce.security.jwt.AccessTokenService;
import com.zaheenmunshi.ecommerce.security.refresh.RefreshTokenCookie;
import com.zaheenmunshi.ecommerce.security.refresh.RefreshTokenService;
import com.zaheenmunshi.ecommerce.user.domain.User;

import jakarta.servlet.http.HttpServletResponse;

/** Starts a login however the user proved who they are (password, signup or Google). */
@Component
public class AuthTokens {

	private final AccessTokenService accessTokens;

	private final RefreshTokenService refreshTokens;

	private final RefreshTokenCookie refreshCookie;

	public AuthTokens(AccessTokenService accessTokens, RefreshTokenService refreshTokens,
			RefreshTokenCookie refreshCookie) {
		this.accessTokens = accessTokens;
		this.refreshTokens = refreshTokens;
		this.refreshCookie = refreshCookie;
	}

	/** Sets the refresh cookie and returns the access token for the response body. */
	public AuthResponse startLogin(User user, HttpServletResponse response) {
		refreshCookie.write(response, refreshTokens.issue(user));
		return AuthResponse.of(accessTokens.issue(user), user);
	}

	/** A fresh access token for the current login, e.g. after the user's roles changed. */
	public AuthResponse reissue(User user) {
		return AuthResponse.of(accessTokens.issue(user), user);
	}

}
