package com.zaheenmunshi.ecommerce.order.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zaheenmunshi.ecommerce.order.dto.OrderResponse;
import com.zaheenmunshi.ecommerce.order.dto.PlaceOrderRequest;
import com.zaheenmunshi.ecommerce.order.service.OrderService;
import com.zaheenmunshi.ecommerce.security.AppUserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping
	ResponseEntity<OrderResponse> place(@AuthenticationPrincipal AppUserPrincipal me,
			@Valid @RequestBody PlaceOrderRequest body) {
		return ResponseEntity.status(HttpStatus.CREATED).body(orderService.place(me.getId(), body));
	}

	@GetMapping
	List<OrderResponse> myOrders(@AuthenticationPrincipal AppUserPrincipal me) {
		return orderService.findForCustomer(me.getId());
	}

}
