package com.zaheenmunshi.ecommerce.order.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zaheenmunshi.ecommerce.common.error.ApiException;
import com.zaheenmunshi.ecommerce.order.domain.CustomerOrder;
import com.zaheenmunshi.ecommerce.order.domain.OrderRepository;
import com.zaheenmunshi.ecommerce.order.dto.OrderResponse;
import com.zaheenmunshi.ecommerce.order.dto.PlaceOrderRequest;
import com.zaheenmunshi.ecommerce.product.domain.Product;
import com.zaheenmunshi.ecommerce.product.domain.ProductRepository;
import com.zaheenmunshi.ecommerce.product.service.ProductService;
import com.zaheenmunshi.ecommerce.user.domain.User;
import com.zaheenmunshi.ecommerce.user.domain.UserRepository;

@Service
public class OrderService {

	private static final int MAX_QUANTITY_PER_ITEM = 9;

	private final OrderRepository orders;

	private final ProductRepository products;

	private final UserRepository users;

	private final ApplicationEventPublisher events;

	public OrderService(OrderRepository orders, ProductRepository products, UserRepository users,
			ApplicationEventPublisher events) {
		this.orders = orders;
		this.products = products;
		this.users = users;
		this.events = events;
	}

	/**
	 * Prices come from the database, stock is reserved in the same transaction and the
	 * product's @Version column turns a concurrent checkout into a 409 instead of overselling.
	 */
	@Transactional
	public OrderResponse place(UUID customerId, PlaceOrderRequest request) {
		User customer = users.findById(customerId).orElseThrow(() -> ApiException.notFound("account not found"));
		Map<LineKey, Integer> quantities = mergeLines(request.items());
		Map<UUID, Product> catalog = products
			.findAllById(quantities.keySet().stream().map(LineKey::productId).distinct().toList())
			.stream()
			.collect(Collectors.toMap(Product::getId, Function.identity()));

		CustomerOrder order = new CustomerOrder(customer, request.address().toShippingAddress());
		quantities.forEach((line, quantity) -> {
			Product product = catalog.get(line.productId());
			if (product == null || product.isDraft()) {
				throw ApiException.badRequest("an item in your cart is no longer available, please remove it");
			}
			String size = checkSize(product, line.size());
			if (product.getStock() < quantity) {
				throw ApiException.conflict("only " + product.getStock() + " left of " + product.getName());
			}
			product.removeStock(quantity);
			order.addItem(product, size, quantity);
		});

		orders.save(order);
		events.publishEvent(new OrderPlacedEvent(order.getId(), customer.getEmail(), customer.getName(),
				order.getTotalAmount()));
		return OrderResponse.from(order);
	}

	@Transactional(readOnly = true)
	public List<OrderResponse> findForCustomer(UUID customerId) {
		return orders.findByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(OrderResponse::from).toList();
	}

	/** The same product and size added twice becomes one line. */
	private static Map<LineKey, Integer> mergeLines(List<PlaceOrderRequest.Line> lines) {
		Map<LineKey, Integer> merged = new LinkedHashMap<>();
		for (PlaceOrderRequest.Line line : lines) {
			String size = line.size() == null || line.size().isBlank() ? null : ProductService.size(line.size()).code();
			merged.merge(new LineKey(line.productId(), size), line.quantity(), Integer::sum);
		}
		if (merged.values().stream().anyMatch(quantity -> quantity > MAX_QUANTITY_PER_ITEM)) {
			throw ApiException.badRequest("you can order at most 9 of an item");
		}
		return merged;
	}

	private static String checkSize(Product product, String size) {
		if (product.getSizes().isEmpty()) {
			return size;
		}
		if (size == null) {
			throw ApiException.badRequest("select a size for " + product.getName());
		}
		if (!product.getSizes().contains(ProductService.size(size))) {
			throw ApiException.badRequest(product.getName() + " is not available in size " + size);
		}
		return size;
	}

	private record LineKey(UUID productId, String size) {
	}

}
