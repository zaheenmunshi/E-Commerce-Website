package com.zaheenmunshi.ecommerce.security.refresh;

import java.time.Duration;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import com.zaheenmunshi.ecommerce.config.AppProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The refresh token travels only in this cookie: HttpOnly (scripts can't read it),
 * SameSite=Strict (never sent cross-site), Secure, and scoped to /api/auth.
 */
@Component
public class RefreshTokenCookie {

	public static final String NAME = "refresh_token";

	private static final String PATH = "/api/auth";

	private final AppProperties.Security settings;

	public RefreshTokenCookie(AppProperties properties) {
		this.settings = properties.security();
	}

	public void write(HttpServletResponse response, String token) {
		response.addHeader(HttpHeaders.SET_COOKIE, cookie(token, settings.refreshTokenTtl()).toString());
	}

	public void clear(HttpServletResponse response) {
		response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
	}

	public Optional<String> read(HttpServletRequest request) {
		return Optional.ofNullable(WebUtils.getCookie(request, NAME))
			.map(Cookie::getValue)
			.filter(value -> !value.isBlank());
	}

	private ResponseCookie cookie(String value, Duration maxAge) {
		return ResponseCookie.from(NAME, value)
			.httpOnly(true)
			.secure(settings.cookieSecure())
			.sameSite("Strict")
			.path(PATH)
			.maxAge(maxAge)
			.build();
	}

}
