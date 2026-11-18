package com.zaheenmunshi.ecommerce.seller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import com.zaheenmunshi.ecommerce.support.IntegrationTest;

class SellerProductFlowIT extends IntegrationTest {

	/** A real 1x1 PNG. */
	private static final byte[] PNG = Base64.getDecoder()
		.decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=");

	@Test
	void customersCannotUseTheSellerApi() throws Exception {
		Login customer = signup();

		mvc.perform(get("/api/seller/products").header("Authorization", customer.bearer()))
			.andExpect(status().isForbidden());
	}

	@Test
	void becomingASellerReturnsATokenWithTheSellerRole() throws Exception {
		Login seller = signupSeller();

		mvc.perform(get("/api/auth/me").header("Authorization", seller.bearer()))
			.andExpect(jsonPath("$.seller").value(true));
		mvc.perform(get("/api/seller/products").header("Authorization", seller.bearer())).andExpect(status().isOk());
	}

	@Test
	void draftsArePrivateUntilPublished() throws Exception {
		Login seller = signupSeller();
		String tag = "tag-" + UUID.randomUUID().toString().substring(0, 8);

		MvcResult draft = mvc.perform(post("/api/seller/products").header("Authorization", seller.bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name":"Linen Shirt","draft":true,"termsAccepted":false}"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.draft").value(true))
			.andReturn();
		String id = json(draft, "$.id");
		mvc.perform(get("/api/products/" + id)).andExpect(status().isNotFound());

		mvc.perform(put("/api/seller/products/" + id).header("Authorization", seller.bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content(fullProduct(tag, "/img/card5.png")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sellingPrice").value(30.0));

		mvc.perform(get("/api/products/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Linen Shirt"));
		mvc.perform(get("/api/products").param("tag", tag.toUpperCase()))
			.andExpect(jsonPath("$[0].id").value(id));
		mvc.perform(get("/api/products/search").param("q", "linen")).andExpect(jsonPath("$[?(@.id == '%s')]".formatted(id)).exists());
	}

	@Test
	void publishingEnforcesTheListingRules() throws Exception {
		Login seller = signupSeller();

		mvc.perform(post("/api/seller/products").header("Authorization", seller.bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content(fullProduct("men", "/img/card5.png").replace("\"stock\":40", "\"stock\":5")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("you should have at least 20 items in stock"));

		mvc.perform(post("/api/seller/products").header("Authorization", seller.bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content(fullProduct("men", "https://evil.example/x.png")))
			.andExpect(status().isBadRequest());
	}

	@Test
	void sellersCannotTouchOtherSellersProducts() throws Exception {
		Login owner = signupSeller();
		Login other = signupSeller();
		MvcResult created = mvc.perform(post("/api/seller/products").header("Authorization", owner.bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content(fullProduct("men", "/img/card5.png")))
			.andExpect(status().isCreated())
			.andReturn();
		String id = json(created, "$.id");

		mvc.perform(put("/api/seller/products/" + id).header("Authorization", other.bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content(fullProduct("men", "/img/card5.png")))
			.andExpect(status().isNotFound());
		mvc.perform(delete("/api/seller/products/" + id).header("Authorization", other.bearer()))
			.andExpect(status().isNotFound());

		mvc.perform(delete("/api/seller/products/" + id).header("Authorization", owner.bearer()))
			.andExpect(status().isNoContent());
	}

	@Test
	void uploadsAcceptRealImagesOnly() throws Exception {
		Login seller = signupSeller();

		MvcResult uploaded = mvc
			.perform(multipart("/api/seller/uploads").file(new MockMultipartFile("image", "shirt.png", "image/png", PNG))
				.header("Authorization", seller.bearer()))
			.andExpect(status().isCreated())
			.andReturn();
		String url = json(uploaded, "$.url");
		assertThat(url).matches("/uploads/[0-9a-f-]{36}\\.png");
		mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG));

		// an HTML file renamed to .png is refused: the type comes from the bytes
		mvc.perform(multipart("/api/seller/uploads")
			.file(new MockMultipartFile("image", "evil.png", "image/png", "<script>alert(1)</script>".getBytes()))
			.header("Authorization", seller.bearer()))
			.andExpect(status().isBadRequest());

		mvc.perform(get("/uploads/..%2F..%2Fpom.xml")).andExpect(status().is4xxClientError());
	}

	private static String fullProduct(String tag, String image) {
		return """
				{"name":"Linen Shirt","shortDescription":"Breathable linen shirt for summer",
				 "description":"A relaxed linen shirt.","images":["%s"],"sizes":["m","l"],
				 "actualPrice":40,"discountPercent":25,"stock":40,"tags":["men","%s"],
				 "termsAccepted":true,"draft":false}""".formatted(image, tag);
	}

}
