package com.zaheenmunshi.ecommerce.product.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create/update payload. Everything except the name may be missing while {@code draft}
 * is true; the publish rules are checked in ProductService. The selling price is
 * always computed on the server from the actual price and discount.
 */
public record ProductRequest(
		@Size(max = 120, message = "product name is too long") String name,
		@Size(max = 100, message = "short description must be between 10 to 100 letters long") String shortDescription,
		@Size(max = 5000, message = "description is too long") String description,
		@Size(max = 4, message = "you can upload at most 4 images") List<@NotBlank String> images,
		List<@NotBlank String> sizes,
		@DecimalMin(value = "0.01", message = "actual price must be more than 0")
		@DecimalMax(value = "99999999.99", message = "actual price is too high")
		@Digits(integer = 8, fraction = 2, message = "price can have at most 2 decimals") BigDecimal actualPrice,
		@Min(value = 0, message = "discount must be between 0 and 100")
		@Max(value = 100, message = "discount must be between 0 and 100") Integer discountPercent,
		@Min(value = 0, message = "stock cannot be negative")
		@Max(value = 1_000_000, message = "stock is too high") Integer stock,
		@Size(max = 20, message = "use at most 20 tags") List<@NotBlank String> tags,
		boolean termsAccepted,
		boolean draft) {
}
