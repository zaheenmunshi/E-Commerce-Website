package com.zaheenmunshi.ecommerce.order.service;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderPlacedEvent(UUID orderId, String customerEmail, String customerName, BigDecimal totalAmount) {
}
