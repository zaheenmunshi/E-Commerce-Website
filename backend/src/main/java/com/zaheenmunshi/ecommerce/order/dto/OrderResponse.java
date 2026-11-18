package com.zaheenmunshi.ecommerce.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.zaheenmunshi.ecommerce.order.domain.CustomerOrder;
import com.zaheenmunshi.ecommerce.order.domain.OrderItem;

public record OrderResponse(UUID id, String status, BigDecimal totalAmount, Instant createdAt, List<Item> items) {

	public record Item(UUID productId, String productName, String size, int quantity, BigDecimal unitPrice) {

		static Item from(OrderItem item) {
			return new Item(item.getProductId(), item.getProductName(), item.getSize(), item.getQuantity(),
					item.getUnitPrice());
		}

	}

	public static OrderResponse from(CustomerOrder order) {
		return new OrderResponse(order.getId(), order.getStatus().name(), order.getTotalAmount(),
				order.getCreatedAt(), order.getItems().stream().map(Item::from).toList());
	}

}
