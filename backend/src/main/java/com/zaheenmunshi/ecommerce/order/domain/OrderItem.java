package com.zaheenmunshi.ecommerce.order.domain;

import java.math.BigDecimal;
import java.util.UUID;

import com.zaheenmunshi.ecommerce.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One order line. Name and price are copied from the product at checkout, and the
 * product is referenced by id only, so orders stay intact if a product is edited or
 * deleted later.
 */
@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private CustomerOrder order;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "product_name", nullable = false, length = 120)
	private String productName;

	@Column(name = "size_code", length = 8)
	private String size;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
	private BigDecimal unitPrice;

	protected OrderItem() {
	}

	OrderItem(CustomerOrder order, UUID productId, String productName, String size, int quantity,
			BigDecimal unitPrice) {
		this.order = order;
		this.productId = productId;
		this.productName = productName;
		this.size = size;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
	}

	public BigDecimal lineTotal() {
		return unitPrice.multiply(BigDecimal.valueOf(quantity));
	}

	public UUID getProductId() {
		return productId;
	}

	public String getProductName() {
		return productName;
	}

	public String getSize() {
		return size;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

}
