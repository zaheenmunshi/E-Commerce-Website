package com.zaheenmunshi.ecommerce.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank(message = "fill all the inputs") String email,
		@NotBlank(message = "fill all the inputs") String password) {
}
