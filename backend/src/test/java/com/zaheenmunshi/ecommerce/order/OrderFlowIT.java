package com.zaheenmunshi.ecommerce.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.zaheenmunshi.ecommerce.support.IntegrationTest;

class OrderFlowIT extends IntegrationTest {

	private static final String ADDRESS = """
			{"address":"12 Lake View","street":"MG Road","city":"Bhopal","state":"MP","pincode":"462001",
			 "landmark":"near the park"}""";

	@Test
	void ordersUseServerPricesAndReserveStock() throws Exception {
		String productId = publishedProduct(25);
		Login customer = signup();

		mvc.perform(post("/api/orders").header("Authorization", customer.bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content(order(productId, "m", 2)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.totalAmount").value(60.0))
			.andExpect(jsonPath("$.items[0].unitPrice").value(30.0))
			.andExpect(jsonPath("$.status").value("PLACED"));

		mvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.stock").value(23));
		mvc.perform(get("/api/orders").header("Authorization", customer.bearer()))
			.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void ordersCannotExceedStockOrUseMissingSizes() throws Exception {
		String productId = publishedProduct(20);
		Login customer = signup();

		mvc.perform(post("/api/orders").header("Authorization", customer.bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content(order(productId, "xxxl", 1)))
			.andExpect(status().isBadRequest());
		mvc.perform(post("/api/orders").header("Authorization", customer.bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content(order(productId, null, 1)))
			.andExpect(status().isBadRequest());
		mvc.perform(post("/api/orders").header("Authorization", customer.bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content(order(productId, "m", 10)))
			.andExpect(status().isBadRequest());
		mvc.perform(post("/api/orders").header("Authorization", customer.bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content(order(UUID.randomUUID().toString(), "m", 1)))
			.andExpect(status().isBadRequest());
	}

	@Test
	void ordersNeedLoginAndAValidAddress() throws Exception {
		String productId = publishedProduct(20);
		mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(order(productId, "m", 1)))
			.andExpect(status().isUnauthorized());

		Login customer = signup();
		mvc.perform(post("/api/orders").header("Authorization", customer.bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content(order(productId, "m", 1).replace("462001", "12")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("enter a valid 6 digit pin code"));
	}

	private String publishedProduct(int stock) throws Exception {
		Login seller = signupSeller();
		MvcResult created = mvc.perform(post("/api/seller/products").header("Authorization", seller.bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name":"Oxford Shirt","shortDescription":"Crisp cotton oxford shirt",
					 "description":"Classic fit.","images":["/img/card8.png"],"sizes":["m","l"],
					 "actualPrice":40,"discountPercent":25,"stock":%d,"tags":["men","shirt"],
					 "termsAccepted":true,"draft":false}""".formatted(stock)))
			.andExpect(status().isCreated())
			.andReturn();
		return json(created, "$.id");
	}

	private static String order(String productId, String size, int quantity) {
		String sizeJson = size == null ? "null" : "\"" + size + "\"";
		return """
				{"items":[{"productId":"%s","size":%s,"quantity":%d}],"address":%s}"""
			.formatted(productId, sizeJson, quantity, ADDRESS);
	}

}
