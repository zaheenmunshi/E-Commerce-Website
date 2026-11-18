package com.zaheenmunshi.ecommerce.order.dto;

import java.util.List;
import java.util.UUID;

import com.zaheenmunshi.ecommerce.order.domain.ShippingAddress;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Only ids, sizes and quantities come from the cart; prices are looked up on the server. */
public record PlaceOrderRequest(
		@NotEmpty(message = "your cart is empty") @Size(max = 50, message = "too many items in one order")
		List<@Valid Line> items,
		@NotNull(message = "fill all the inputs first") @Valid Address address) {

	public record Line(@NotNull UUID productId, String size,
			@Min(value = 1, message = "quantity must be between 1 and 9")
			@Max(value = 9, message = "quantity must be between 1 and 9") int quantity) {
	}

	public record Address(
			@NotBlank(message = "fill all the inputs first") @Size(max = 200) String address,
			@NotBlank(message = "fill all the inputs first") @Size(max = 200) String street,
			@NotBlank(message = "fill all the inputs first") @Size(max = 100) String city,
			@NotBlank(message = "fill all the inputs first") @Size(max = 100) String state,
			@NotBlank(message = "fill all the inputs first")
			@Pattern(regexp = "\\d{6}", message = "enter a valid 6 digit pin code") String pincode,
			@NotBlank(message = "fill all the inputs first") @Size(max = 200) String landmark) {

		public ShippingAddress toShippingAddress() {
			return new ShippingAddress(address.trim(), street.trim(), city.trim(), state.trim(), pincode,
					landmark.trim());
		}

	}

}
