package com.zaheenmunshi.ecommerce.seller.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SellerApplicationRequest(
		@NotBlank(message = "enter your business name")
		@Size(max = 120, message = "business name is too long") String businessName,
		@NotBlank(message = "enter your business address")
		@Size(max = 500, message = "address is too long") String address,
		@NotBlank(message = "tell us about your business")
		@Size(max = 2000, message = "about is too long") String about,
		@NotBlank(message = "enter your phone number")
		@Pattern(regexp = "\\d{10,15}", message = "invalid number, please enter valid one") String phone,
		@AssertTrue(message = "you must agree to our terms and conditions") boolean termsAccepted,
		@AssertTrue(message = "you must confirm all information is legit") boolean informationConfirmed) {
}
