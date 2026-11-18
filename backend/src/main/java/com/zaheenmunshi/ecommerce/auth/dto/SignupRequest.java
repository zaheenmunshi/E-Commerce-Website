package com.zaheenmunshi.ecommerce.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
		@NotNull(message = "name must be 3 letters long")
		@Size(min = 3, max = 100, message = "name must be 3 to 100 letters long") String name,
		@NotBlank(message = "enter your email")
		@Email(message = "enter a valid email") @Size(max = 254, message = "email is too long") String email,
		@NotNull(message = "password should be 8 letters long")
		@Size(min = 8, max = 72, message = "password should be 8 to 72 characters long") String password,
		@NotBlank(message = "enter your phone number")
		@Pattern(regexp = "\\d{10,15}", message = "invalid number, please enter valid one") String phone,
		@AssertTrue(message = "you must agree to our terms and conditions") boolean termsAccepted,
		boolean marketingOptIn) {
}
