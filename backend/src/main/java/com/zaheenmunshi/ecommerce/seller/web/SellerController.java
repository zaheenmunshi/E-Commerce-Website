package com.zaheenmunshi.ecommerce.seller.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.zaheenmunshi.ecommerce.auth.dto.AuthResponse;
import com.zaheenmunshi.ecommerce.auth.service.AuthTokens;
import com.zaheenmunshi.ecommerce.product.dto.ProductRequest;
import com.zaheenmunshi.ecommerce.product.dto.ProductResponse;
import com.zaheenmunshi.ecommerce.product.service.ProductService;
import com.zaheenmunshi.ecommerce.security.AppUserPrincipal;
import com.zaheenmunshi.ecommerce.seller.dto.SellerApplicationRequest;
import com.zaheenmunshi.ecommerce.seller.service.SellerService;
import com.zaheenmunshi.ecommerce.storage.service.ImageUploadService;

import jakarta.validation.Valid;

/**
 * Seller dashboard API. {@code /apply} is open to any logged-in user; everything else
 * needs ROLE_SELLER from the access token (see SecurityConfig) and only touches the seller's own products.
 */
@RestController
@RequestMapping("/api/seller")
public class SellerController {

	private final SellerService sellerService;

	private final ProductService productService;

	private final ImageUploadService imageUploadService;

	private final AuthTokens authTokens;

	public SellerController(SellerService sellerService, ProductService productService,
			ImageUploadService imageUploadService, AuthTokens authTokens) {
		this.sellerService = sellerService;
		this.productService = productService;
		this.imageUploadService = imageUploadService;
		this.authTokens = authTokens;
	}

	/** Returns a new access token, because the old one does not carry the SELLER role yet. */
	@PostMapping("/apply")
	AuthResponse apply(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody SellerApplicationRequest body) {
		return authTokens.reissue(sellerService.becomeSeller(me.getId(), body));
	}

	@GetMapping("/products")
	List<ProductResponse> myProducts(@AuthenticationPrincipal AppUserPrincipal me) {
		return productService.findForSeller(me.getId());
	}

	@GetMapping("/products/{id}")
	ProductResponse myProduct(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable UUID id) {
		return productService.getForSeller(me.getId(), id);
	}

	@PostMapping("/products")
	ResponseEntity<ProductResponse> create(@AuthenticationPrincipal AppUserPrincipal me,
			@Valid @RequestBody ProductRequest body) {
		return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(me.getId(), body));
	}

	@PutMapping("/products/{id}")
	ProductResponse update(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable UUID id,
			@Valid @RequestBody ProductRequest body) {
		return productService.update(me.getId(), id, body);
	}

	@DeleteMapping("/products/{id}")
	ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable UUID id) {
		productService.delete(me.getId(), id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping(path = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ResponseEntity<Map<String, String>> upload(@RequestParam("image") MultipartFile image) {
		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("url", imageUploadService.upload(image)));
	}

}
