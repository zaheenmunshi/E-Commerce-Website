package com.zaheenmunshi.ecommerce.security.guard;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.web.authentication.password.HaveIBeenPwnedRestApiPasswordChecker;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.zaheenmunshi.ecommerce.common.error.ApiException;
import com.zaheenmunshi.ecommerce.config.AppProperties;

/**
 * Password rules beyond length (NIST SP 800-63B style): fits bcrypt's 72-byte limit,
 * does not contain the email name, and is not in a known data breach. The breach check
 * sends only the first 5 hex chars of the SHA-1 hash (k-anonymity); if the service is
 * unreachable, signup is not blocked.
 */
@Component
public class PasswordPolicy {

	private static final Logger log = LoggerFactory.getLogger(PasswordPolicy.class);

	private static final int BCRYPT_MAX_BYTES = 72;

	private final CompromisedPasswordChecker breachChecker;

	public PasswordPolicy(AppProperties properties) {
		this.breachChecker = properties.security().checkBreachedPasswords() ? haveIBeenPwned() : null;
	}

	public void check(String password, String email) {
		if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
			throw ApiException.badRequest("password is too long");
		}
		String emailName = email.substring(0, Math.max(0, email.indexOf('@'))).toLowerCase(Locale.ROOT);
		if (emailName.length() >= 3 && password.toLowerCase(Locale.ROOT).contains(emailName)) {
			throw ApiException.badRequest("password must not contain your email name");
		}
		if (breachChecker != null && isBreached(password)) {
			throw ApiException.badRequest("this password appeared in a data breach, please choose another one");
		}
	}

	private boolean isBreached(String password) {
		try {
			return breachChecker.check(password).isCompromised();
		}
		catch (RuntimeException ex) {
			log.warn("Breached-password check unavailable, allowing signup: {}", ex.getMessage());
			return false;
		}
	}

	private static CompromisedPasswordChecker haveIBeenPwned() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(3));
		requestFactory.setReadTimeout(Duration.ofSeconds(3));
		HaveIBeenPwnedRestApiPasswordChecker checker = new HaveIBeenPwnedRestApiPasswordChecker();
		checker.setRestClient(RestClient.builder()
			.baseUrl("https://api.pwnedpasswords.com/range/")
			.requestFactory(requestFactory)
			.build());
		return checker;
	}

}
