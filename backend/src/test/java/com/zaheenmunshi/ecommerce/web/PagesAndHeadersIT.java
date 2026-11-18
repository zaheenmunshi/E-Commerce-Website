package com.zaheenmunshi.ecommerce.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.zaheenmunshi.ecommerce.support.IntegrationTest;

class PagesAndHeadersIT extends IntegrationTest {

	@Test
	void prettyUrlsForwardToTheStaticPages() throws Exception {
		mvc.perform(get("/products/" + UUID.randomUUID())).andExpect(forwardedUrl("/product.html"));
		mvc.perform(get("/add-product/" + UUID.randomUUID())).andExpect(forwardedUrl("/addProduct.html"));
		mvc.perform(get("/search").param("q", "shirt")).andExpect(forwardedUrl("/search.html"));
		mvc.perform(get("/checkout")).andExpect(status().isOk()).andExpect(forwardedUrl("/checkout.html"));
		mvc.perform(get("/404")).andExpect(status().isNotFound());
	}

	@Test
	void responsesCarrySecurityHeaders() throws Exception {
		mvc.perform(get("/api/products").param("tag", "men"))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
			.andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
			.andExpect(header().string("X-Content-Type-Options", "nosniff"))
			.andExpect(header().string("X-Frame-Options", "DENY"))
			.andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
			.andExpect(header().string("Permissions-Policy", containsString("camera=()")))
			.andExpect(header().string("Cross-Origin-Opener-Policy", "same-origin"));
	}

	@Test
	void onlyTheHealthActuatorIsReachable() throws Exception {
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mvc.perform(get("/actuator/env")).andExpect(status().is4xxClientError());
	}

}
