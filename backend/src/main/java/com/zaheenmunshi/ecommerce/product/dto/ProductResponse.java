package com.zaheenmunshi.ecommerce.product.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.zaheenmunshi.ecommerce.product.domain.Product;
import com.zaheenmunshi.ecommerce.product.domain.ProductSize;

public record ProductResponse(UUID id, String name, String shortDescription, String description, List<String> images,
		List<String> sizes, BigDecimal actualPrice, Integer discountPercent, BigDecimal sellingPrice, Integer stock,
		List<String> tags, boolean draft) {

	public static ProductResponse from(Product product) {
		return new ProductResponse(product.getId(), product.getName(), product.getShortDescription(),
				product.getDescription(), List.copyOf(product.getImages()),
				product.getSizes().stream().sorted().map(ProductSize::code).toList(), product.getActualPrice(),
				product.getDiscountPercent(), product.getSellingPrice(), product.getStock(),
				List.copyOf(product.getTags()), product.isDraft());
	}

}
