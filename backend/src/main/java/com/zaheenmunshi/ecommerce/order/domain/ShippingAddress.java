package com.zaheenmunshi.ecommerce.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record ShippingAddress(
		@Column(name = "ship_address", nullable = false, length = 200) String address,
		@Column(name = "ship_street", nullable = false, length = 200) String street,
		@Column(name = "ship_city", nullable = false, length = 100) String city,
		@Column(name = "ship_state", nullable = false, length = 100) String state,
		@Column(name = "ship_pincode", nullable = false, length = 6) String pincode,
		@Column(name = "ship_landmark", nullable = false, length = 200) String landmark) {
}
