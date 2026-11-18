package com.zaheenmunshi.ecommerce.security.guard;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.zaheenmunshi.ecommerce.common.error.ApiException;
import com.zaheenmunshi.ecommerce.config.AppProperties;

/**
 * Brute-force protection per account: after {@code maxLoginFailures} wrong passwords
 * within {@code loginLockout}, further logins for that email are refused until the
 * window passes. In memory, so it is per app instance (fine for one container).
 */
@Service
public class LoginAttemptService {

	private final Map<String, Failures> failures = new ConcurrentHashMap<>();

	private final int maxFailures;

	private final Duration lockout;

	private final Clock clock;

	public LoginAttemptService(AppProperties properties, Clock clock) {
		this.maxFailures = properties.security().maxLoginFailures();
		this.lockout = properties.security().loginLockout();
		this.clock = clock;
	}

	public void checkAllowed(String email) {
		Failures current = failures.get(email);
		if (current != null && current.count() >= maxFailures && !current.expired(clock.instant(), lockout)) {
			throw ApiException.tooManyRequests(
					"too many failed logins, try again in " + lockout.toMinutes() + " minutes");
		}
	}

	public void recordFailure(String email) {
		Instant now = clock.instant();
		failures.merge(email, new Failures(1, now),
				(old, first) -> old.expired(now, lockout) ? first : new Failures(old.count() + 1, old.since()));
	}

	public void recordSuccess(String email) {
		failures.remove(email);
	}

	@Scheduled(fixedDelay = 10, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
	void forgetOldFailures() {
		Instant now = clock.instant();
		failures.values().removeIf(entry -> entry.expired(now, lockout));
	}

	private record Failures(int count, Instant since) {

		boolean expired(Instant now, Duration window) {
			return since.plus(window).isBefore(now);
		}

	}

}
