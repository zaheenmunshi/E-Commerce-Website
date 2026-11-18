package com.zaheenmunshi.ecommerce.config;

import java.nio.file.Path;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app")
public record AppProperties(
		@DefaultValue Security security,
		@DefaultValue Storage storage,
		@DefaultValue Mail mail,
		@DefaultValue DemoData demoData) {

	/**
	 * @param refreshTokenTtl sliding lifetime of a refresh token; every refresh renews it
	 * @param sessionMaxAge absolute cap of one login, however often it is refreshed
	 * @param cookieSecure send the refresh cookie over HTTPS only; false only for plain-HTTP dev
	 * @param checkBreachedPasswords reject passwords found in the Have I Been Pwned corpus
	 * @param authRequestsPerMinute per-IP limit for login and signup calls
	 */
	public record Security(
			@DefaultValue Jwt jwt,
			@DefaultValue("7d") Duration refreshTokenTtl,
			@DefaultValue("30d") Duration sessionMaxAge,
			@DefaultValue("true") boolean cookieSecure,
			@DefaultValue("true") boolean checkBreachedPasswords,
			@DefaultValue("5") int maxLoginFailures,
			@DefaultValue("15m") Duration loginLockout,
			@DefaultValue("20") int authRequestsPerMinute) {
	}

	/**
	 * @param secret base64 HMAC key of at least 256 bits; blank means a random key per start (dev only)
	 */
	public record Jwt(
			@DefaultValue("ecommerce-backend") String issuer,
			@DefaultValue("ecommerce-web") String audience,
			String secret,
			@DefaultValue("15m") Duration accessTokenTtl) {
	}

	/**
	 * Where product images go: {@code local} disk for development, {@code s3} on AWS
	 * (containers have no persistent disk).
	 */
	public record Storage(@DefaultValue("local") String type, @DefaultValue("./data/uploads") Path uploadDir,
			String s3Bucket) {

		public Path root() {
			return uploadDir.toAbsolutePath().normalize();
		}

	}

	public record Mail(boolean enabled, @DefaultValue("no-reply@clothing.local") String from) {
	}

	public record DemoData(boolean enabled, @DefaultValue("seller@demo.local") String sellerEmail,
			String sellerPassword) {
	}

}
