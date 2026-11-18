package com.zaheenmunshi.ecommerce.order.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.zaheenmunshi.ecommerce.common.domain.BaseEntity;
import com.zaheenmunshi.ecommerce.product.domain.Product;
import com.zaheenmunshi.ecommerce.user.domain.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/** Named CustomerOrder because ORDER is a reserved word in SQL. */
@Entity
@Table(name = "orders", indexes = @Index(name = "ix_orders_customer", columnList = "customer_id"))
public class CustomerOrder extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private User customer;

	@Embedded
	private ShippingAddress shippingAddress;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status = OrderStatus.PLACED;

	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount = BigDecimal.ZERO;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	protected CustomerOrder() {
	}

	public CustomerOrder(User customer, ShippingAddress shippingAddress) {
		this.customer = customer;
		this.shippingAddress = shippingAddress;
	}

	public void addItem(Product product, String size, int quantity) {
		OrderItem item = new OrderItem(this, product.getId(), product.getName(), size, quantity,
				product.getSellingPrice());
		items.add(item);
		totalAmount = totalAmount.add(item.lineTotal());
	}

	public User getCustomer() {
		return customer;
	}

	public ShippingAddress getShippingAddress() {
		return shippingAddress;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public List<OrderItem> getItems() {
		return items;
	}

}
