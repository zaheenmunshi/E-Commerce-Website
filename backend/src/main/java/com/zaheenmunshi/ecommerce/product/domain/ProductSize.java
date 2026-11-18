package com.zaheenmunshi.ecommerce.product.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Clothing sizes; the API uses the lower-case codes ("xs" ... "xxxl"). */
public enum ProductSize {

	XS, S, M, L, XL, XXL, XXXL;

	public String code() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static Optional<ProductSize> parse(String code) {
		String normalized = code == null ? null : code.trim().toLowerCase(Locale.ROOT);
		return Arrays.stream(values()).filter(size -> size.code().equals(normalized)).findFirst();
	}

}
