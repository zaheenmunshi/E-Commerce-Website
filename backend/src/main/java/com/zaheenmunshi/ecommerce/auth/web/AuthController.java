package com.zaheenmunshi.ecommerce.auth.web;

import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zaheenmunshi.ecommerce.auth.dto.AuthResponse;
import com.zaheenmunshi.ecommerce.auth.dto.LoginRequest;
import com.zaheenmunshi.ecommerce.auth.dto.SignupRequest;
import com.zaheenmunshi.ecommerce.auth.dto.UserResponse;
import com.zaheenmunshi.ecommerce.auth.service.AuthService;
import com.zaheenmunshi.ecommerce.auth.service.AuthTokens;
import com.zaheenmunshi.ecommerce.security.AppUserPrincipal;
import com.zaheenmunshi.ecommerce.security.refresh.RefreshTokenCookie;
import com.zaheenmunshi.ecommerce.security.refresh.RefreshTokenService.Rotation;
import com.zaheenmunshi.ecommerce.security.refresh.RefreshTokenService;
import com.zaheenmunshi.ecommerce.user.domain.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * Token endpoints. Login and signup return an access token (JWT, for the
 * {@code Authorization: Bearer} header) and set the refresh-token cookie; /refresh trades
 * that cookie for a new pair; /logout revokes it.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	private final AuthTokens authTokens;

	private final RefreshTokenService refreshTokens;

	private final RefreshTokenCookie refreshCookie;

	private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;

	public AuthController(AuthService authService, AuthTokens authTokens, RefreshTokenService refreshTokens,
			RefreshTokenCookie refreshCookie, ObjectProvider<ClientRegistrationRepository> clientRegistrations) {
		this.authService = authService;
		this.authTokens = authTokens;
		this.refreshTokens = refreshTokens;
		this.refreshCookie = refreshCookie;
		this.clientRegistrations = clientRegistrations;
	}

	@PostMapping("/signup")
	ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest body, HttpServletResponse response) {
		User user = authService.register(body);
		return ResponseEntity.status(HttpStatus.CREATED).body(authTokens.startLogin(user, response));
	}

	@PostMapping("/login")
	AuthResponse login(@Valid @RequestBody LoginRequest body, HttpServletResponse response) {
		return authTokens.startLogin(authService.login(body.email(), body.password()), response);
	}

	/** 200 with new tokens, 204 when there is no login at all, 401 when it expired or was revoked. */
	@PostMapping("/refresh")
	ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
		String cookie = refreshCookie.read(request).orElse(null);
		if (cookie == null) {
			return ResponseEntity.noContent().build();
		}
		return switch (refreshTokens.rotate(cookie)) {
			case Rotation.Rotated(String token, User user) -> {
				refreshCookie.write(response, token);
				yield ResponseEntity.ok(authTokens.reissue(user));
			}
			case Rotation.Retry _ -> ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "token was just refreshed, retry"));
			case Rotation.Invalid _ -> {
				refreshCookie.clear(response);
				yield ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "please log in again"));
			}
		};
	}

	@PostMapping("/logout")
	ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
		refreshCookie.read(request).ifPresent(refreshTokens::revoke);
		refreshCookie.clear(response);
		return ResponseEntity.noContent().build();
	}

	/** 200 with the user (fresh from the database) for a valid token, 204 when anonymous. */
	@GetMapping("/me")
	ResponseEntity<UserResponse> me(@AuthenticationPrincipal AppUserPrincipal principal) {
		return principal == null ? ResponseEntity.noContent().build()
				: ResponseEntity.ok(UserResponse.from(authService.get(principal.getId())));
	}

	/** Which "Continue with ..." buttons the login page should show, e.g. ["google"]. */
	@GetMapping("/providers")
	List<String> providers() {
		ClientRegistrationRepository registrations = clientRegistrations.getIfAvailable();
		if (!(registrations instanceof Iterable<?> iterable)) {
			return List.of();
		}
		return StreamSupport.stream(iterable.spliterator(), false)
			.map(registration -> ((ClientRegistration) registration).getRegistrationId())
			.sorted()
			.toList();
	}

}
