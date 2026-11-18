package com.zaheenmunshi.ecommerce.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

/**
 * Base for full-stack tests: real Spring context, H2 in PostgreSQL mode, MockMvc, and a
 * clock the test can move. Every test signs up its own users, so tests don't interfere.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTest.TestClockConfig.class)
public abstract class IntegrationTest {

	protected static final String PASSWORD = "Str0ng-and-l0ng!";

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected MutableClock clock;

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	/** A logged-in user: their access token and refresh cookie. */
	protected record Login(String email, String accessToken, Cookie refreshCookie) {

		public String bearer() {
			return "Bearer " + accessToken;
		}

	}

	protected static String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@example.com";
	}

	/** A POST to /api/auth/*, which must look like a same-origin script call. */
	protected static MockHttpServletRequestBuilder authPost(String path) {
		return post(path).header("X-Requested-With", "fetch").contentType(MediaType.APPLICATION_JSON);
	}

	protected Login signup() throws Exception {
		String email = uniqueEmail();
		MvcResult result = mvc.perform(authPost("/api/auth/signup").content("""
				{"name":"Test Shopper","email":"%s","password":"%s","phone":"9876543210",
				 "termsAccepted":true,"marketingOptIn":false}""".formatted(email, PASSWORD)))
			.andExpect(status().isCreated())
			.andReturn();
		return toLogin(email, result);
	}

	protected Login signupSeller() throws Exception {
		Login customer = signup();
		MvcResult result = mvc.perform(post("/api/seller/apply").header("Authorization", customer.bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"businessName":"Test Store","address":"1 Market Road, Bhopal","about":"We sell shirts",
					 "phone":"9876543210","termsAccepted":true,"informationConfirmed":true}"""))
			.andExpect(status().isOk())
			.andReturn();
		return new Login(customer.email(), JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken"),
				customer.refreshCookie());
	}

	protected static Login toLogin(String email, MvcResult result) throws Exception {
		return new Login(email, JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken"),
				result.getResponse().getCookie("refresh_token"));
	}

	protected static String json(MvcResult result, String path) throws Exception {
		Object value = JsonPath.read(result.getResponse().getContentAsString(), path);
		return String.valueOf(value);
	}

	protected MvcResult getJson(String path, Login login) throws Exception {
		MockHttpServletRequestBuilder request = get(path);
		if (login != null) {
			request.header("Authorization", login.bearer());
		}
		return mvc.perform(request).andReturn();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestClockConfig {

		/** Replaces the system clock everywhere: tokens, lockouts and JWT expiry checks. */
		@Bean
		@Primary
		MutableClock testClock() {
			return new MutableClock();
		}

	}

}
