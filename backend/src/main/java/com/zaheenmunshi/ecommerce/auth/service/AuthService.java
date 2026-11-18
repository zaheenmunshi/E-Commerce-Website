package com.zaheenmunshi.ecommerce.auth.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zaheenmunshi.ecommerce.auth.dto.SignupRequest;
import com.zaheenmunshi.ecommerce.common.error.ApiException;
import com.zaheenmunshi.ecommerce.security.AppUserPrincipal;
import com.zaheenmunshi.ecommerce.security.guard.LoginAttemptService;
import com.zaheenmunshi.ecommerce.security.guard.PasswordPolicy;
import com.zaheenmunshi.ecommerce.user.domain.User;
import com.zaheenmunshi.ecommerce.user.domain.UserRepository;

/** Email + password accounts. Tokens are issued by {@link AuthTokens}. */
@Service
public class AuthService {

	private final UserRepository users;

	private final PasswordEncoder passwordEncoder;

	private final PasswordPolicy passwordPolicy;

	private final AuthenticationManager authenticationManager;

	private final LoginAttemptService loginAttempts;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder, PasswordPolicy passwordPolicy,
			AuthenticationManager authenticationManager, LoginAttemptService loginAttempts) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.passwordPolicy = passwordPolicy;
		this.authenticationManager = authenticationManager;
		this.loginAttempts = loginAttempts;
	}

	@Transactional
	public User register(SignupRequest request) {
		String email = normalizeEmail(request.email());
		passwordPolicy.check(request.password(), email);
		if (users.existsByEmail(email)) {
			throw ApiException.conflict("email already exists");
		}
		return users.save(new User(request.name().trim(), email, passwordEncoder.encode(request.password()),
				request.phone(), request.marketingOptIn()));
	}

	/** @throws AuthenticationException on a wrong email/password (shown as one generic message) */
	@Transactional(readOnly = true)
	public User login(String rawEmail, String password) {
		String email = normalizeEmail(rawEmail);
		loginAttempts.checkAllowed(email);
		UUID userId;
		try {
			userId = ((AppUserPrincipal) authenticationManager
				.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password))
				.getPrincipal()).getId();
		}
		catch (AuthenticationException ex) {
			loginAttempts.recordFailure(email);
			throw ex;
		}
		loginAttempts.recordSuccess(email);
		return users.findById(userId).orElseThrow(() -> ApiException.unauthorized("account not found"));
	}

	@Transactional(readOnly = true)
	public User get(UUID userId) {
		return users.findById(userId).orElseThrow(() -> ApiException.unauthorized("account not found"));
	}

	public static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
