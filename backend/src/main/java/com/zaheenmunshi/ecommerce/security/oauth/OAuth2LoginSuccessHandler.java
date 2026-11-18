package com.zaheenmunshi.ecommerce.security.oauth;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.zaheenmunshi.ecommerce.common.error.ApiException;
import com.zaheenmunshi.ecommerce.security.refresh.RefreshTokenCookie;
import com.zaheenmunshi.ecommerce.security.refresh.RefreshTokenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Ends the OAuth 2.0 authorization-code flow: links the account, sets our own refresh
 * cookie and sends the browser home, where the page exchanges it for an access token.
 * The provider's tokens are not kept, and the temporary login session is discarded.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

	private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

	private final OAuthAccountService accounts;

	private final RefreshTokenService refreshTokens;

	private final RefreshTokenCookie refreshCookie;

	public OAuth2LoginSuccessHandler(OAuthAccountService accounts, RefreshTokenService refreshTokens,
			RefreshTokenCookie refreshCookie) {
		this.accounts = accounts;
		this.refreshTokens = refreshTokens;
		this.refreshCookie = refreshCookie;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		try {
			refreshCookie.write(response,
					refreshTokens.issue(accounts.findOrCreate((OAuth2AuthenticationToken) authentication)));
			response.sendRedirect("/");
		}
		catch (ApiException ex) {
			log.info("OAuth login refused: {}", ex.getMessage());
			response.sendRedirect("/login?error=oauth");
		}
		finally {
			HttpSession session = request.getSession(false);
			if (session != null) {
				session.invalidate();
			}
		}
	}

}
