package com.zaheenmunshi.ecommerce.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.zaheenmunshi.ecommerce.support.IntegrationTest;

import jakarta.servlet.http.Cookie;

class AuthFlowIT extends IntegrationTest {

	@Test
	void signupReturnsAccessTokenAndSetsHardenedRefreshCookie() throws Exception {
		Login login = signup();

		assertThat(login.accessToken().split("\\.")).hasSize(3);
		Cookie cookie = login.refreshCookie();
		assertThat(cookie.isHttpOnly()).isTrue();
		assertThat(cookie.getPath()).isEqualTo("/api/auth");
		assertThat(cookie.getAttribute("SameSite")).isEqualTo("Strict");

		mvc.perform(get("/api/auth/me").header("Authorization", login.bearer()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value(login.email()))
			.andExpect(jsonPath("$.seller").value(false));
	}

	@Test
	void meIsEmptyForAnonymousVisitors() throws Exception {
		mvc.perform(get("/api/auth/me")).andExpect(status().isNoContent());
	}

	@Test
	void protectedApiNeedsAValidBearerToken() throws Exception {
		mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized())
			.andExpect(header().exists("WWW-Authenticate"));
		mvc.perform(get("/api/orders").header("Authorization", "Bearer not-a-jwt")).andExpect(status().isUnauthorized());

		Login login = signup();
		String tampered = login.accessToken().substring(0, login.accessToken().length() - 2) + "xx";
		mvc.perform(get("/api/orders").header("Authorization", "Bearer " + tampered))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void accessTokenExpiresAfterFifteenMinutes() throws Exception {
		Login login = signup();
		clock.advance(Duration.ofMinutes(16));

		mvc.perform(get("/api/orders").header("Authorization", login.bearer())).andExpect(status().isUnauthorized());
	}

	@Test
	void loginWithWrongPasswordIsGenericAndLocksAfterFiveFailures() throws Exception {
		Login login = signup();
		String wrong = """
				{"email":"%s","password":"wrong-password"}""".formatted(login.email());

		for (int attempt = 0; attempt < 5; attempt++) {
			mvc.perform(authPost("/api/auth/login").content(wrong))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("email or password is incorrect"));
		}
		mvc.perform(authPost("/api/auth/login").content("""
				{"email":"%s","password":"%s"}""".formatted(login.email(), PASSWORD)))
			.andExpect(status().isTooManyRequests());

		clock.advance(Duration.ofMinutes(16));
		mvc.perform(authPost("/api/auth/login").content("""
				{"email":"%s","password":"%s"}""".formatted(login.email().toUpperCase(), PASSWORD)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tokenType").value("Bearer"));
	}

	@Test
	void authEndpointsRejectCrossSiteRequests() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isForbidden());
		mvc.perform(authPost("/api/auth/refresh").header("Sec-Fetch-Site", "cross-site"))
			.andExpect(status().isForbidden());
	}

	@Test
	void signupValidatesInputAndRejectsDuplicates() throws Exception {
		mvc.perform(authPost("/api/auth/signup").content("""
				{"name":"Al","email":"x@example.com","password":"Str0ng-and-l0ng!","phone":"9876543210",
				 "termsAccepted":true}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("name must be 3 to 100 letters long"));

		Login existing = signup();
		mvc.perform(authPost("/api/auth/signup").content("""
				{"name":"Someone","email":"%s","password":"%s","phone":"9876543210","termsAccepted":true}"""
			.formatted(existing.email(), PASSWORD)))
			.andExpect(status().isConflict());
	}

	@Test
	void refreshRotatesTheTokenAndDetectsReuse() throws Exception {
		Login login = signup();
		Cookie first = login.refreshCookie();

		MvcResult refreshed = mvc.perform(authPost("/api/auth/refresh").cookie(first))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andReturn();
		Cookie second = refreshed.getResponse().getCookie("refresh_token");
		assertThat(second.getValue()).isNotEqualTo(first.getValue());

		// a second tab using the old cookie in the same moment is told to retry, not logged out
		mvc.perform(authPost("/api/auth/refresh").cookie(first)).andExpect(status().isConflict());

		// the same old cookie much later means it was stolen: the whole login is revoked
		clock.advance(Duration.ofMinutes(5));
		mvc.perform(authPost("/api/auth/refresh").cookie(first)).andExpect(status().isUnauthorized());
		mvc.perform(authPost("/api/auth/refresh").cookie(second)).andExpect(status().isUnauthorized());
	}

	@Test
	void refreshWithoutCookieMeansAnonymous() throws Exception {
		mvc.perform(authPost("/api/auth/refresh")).andExpect(status().isNoContent());
	}

	@Test
	void logoutRevokesTheRefreshToken() throws Exception {
		Login login = signup();

		mvc.perform(authPost("/api/auth/logout").cookie(login.refreshCookie())).andExpect(status().isNoContent());

		mvc.perform(authPost("/api/auth/refresh").cookie(login.refreshCookie())).andExpect(status().isUnauthorized());
	}

}
