package com.zaheenmunshi.ecommerce.product.web;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zaheenmunshi.ecommerce.product.dto.ProductResponse;
import com.zaheenmunshi.ecommerce.product.service.ProductService;

/** Public storefront reads; only published products are visible here. */
@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	List<ProductResponse> byTag(@RequestParam String tag) {
		return productService.findByTag(tag);
	}

	@GetMapping("/search")
	List<ProductResponse> search(@RequestParam("q") String query) {
		return productService.search(query);
	}

	@GetMapping("/{id}")
	ProductResponse get(@PathVariable UUID id) {
		return productService.getPublished(id);
	}

	@GetMapping("/{id}/similar")
	List<ProductResponse> similar(@PathVariable UUID id) {
		return productService.findSimilar(id);
	}

}
