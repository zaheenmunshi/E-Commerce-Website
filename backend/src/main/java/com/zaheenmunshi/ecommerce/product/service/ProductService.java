package com.zaheenmunshi.ecommerce.product.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zaheenmunshi.ecommerce.common.error.ApiException;
import com.zaheenmunshi.ecommerce.product.domain.Product;
import com.zaheenmunshi.ecommerce.product.domain.ProductRepository;
import com.zaheenmunshi.ecommerce.product.domain.ProductSize;
import com.zaheenmunshi.ecommerce.product.dto.ProductRequest;
import com.zaheenmunshi.ecommerce.product.dto.ProductResponse;
import com.zaheenmunshi.ecommerce.user.domain.User;
import com.zaheenmunshi.ecommerce.user.domain.UserRepository;

@Service
public class ProductService {

	private static final int MAX_RESULTS = 50;

	private static final int MIN_STOCK_TO_PUBLISH = 20;

	/** Only images we host: uploads, or the bundled demo pictures. */
	private static final Pattern IMAGE_PATH = Pattern.compile("^/(uploads|img)/[A-Za-z0-9._-]{1,200}$");

	private final ProductRepository products;

	private final UserRepository users;

	public ProductService(ProductRepository products, UserRepository users) {
		this.products = products;
		this.users = users;
	}

	// ---- storefront (published products only) ----

	@Transactional(readOnly = true)
	public List<ProductResponse> findByTag(String tag) {
		return toResponses(products.findPublishedByTag(normalizeTag(tag), firstPage()));
	}

	@Transactional(readOnly = true)
	public List<ProductResponse> search(String query) {
		String term = normalizeTag(query);
		if (term.isEmpty()) {
			return List.of();
		}
		String pattern = "%" + term.replace("\\", "").replace("%", "").replace("_", "") + "%";
		return toResponses(products.searchPublished(term, pattern, firstPage()));
	}

	@Transactional(readOnly = true)
	public ProductResponse getPublished(UUID id) {
		return ProductResponse.from(findPublished(id));
	}

	@Transactional(readOnly = true)
	public List<ProductResponse> findSimilar(UUID id) {
		Product product = findPublished(id);
		if (product.getTags().isEmpty()) {
			return List.of();
		}
		return toResponses(products.findPublishedSharingTags(id, product.getTags(), PageRequest.of(0, 12)));
	}

	// ---- seller dashboard (own products, drafts included) ----

	@Transactional(readOnly = true)
	public List<ProductResponse> findForSeller(UUID sellerId) {
		return toResponses(products.findBySellerIdOrderByCreatedAtDesc(sellerId));
	}

	@Transactional(readOnly = true)
	public ProductResponse getForSeller(UUID sellerId, UUID id) {
		return ProductResponse.from(findOwned(sellerId, id));
	}

	@Transactional
	public ProductResponse create(UUID sellerId, ProductRequest request) {
		User seller = users.getReferenceById(sellerId);
		Product product = new Product(seller);
		apply(request, product);
		return ProductResponse.from(products.save(product));
	}

	@Transactional
	public ProductResponse update(UUID sellerId, UUID id, ProductRequest request) {
		Product product = findOwned(sellerId, id);
		apply(request, product);
		return ProductResponse.from(product);
	}

	@Transactional
	public void delete(UUID sellerId, UUID id) {
		products.delete(findOwned(sellerId, id));
	}

	private void apply(ProductRequest request, Product product) {
		if (!request.draft()) {
			validateForPublishing(request);
		}
		else if (isBlank(request.name())) {
			throw ApiException.badRequest("enter product name");
		}
		List<String> images = nullToEmpty(request.images()).stream().map(String::trim).toList();
		if (images.stream().anyMatch(image -> !IMAGE_PATH.matcher(image).matches())) {
			throw ApiException.badRequest("images must be uploaded through the image upload");
		}
		product.setName(request.name().trim());
		product.setShortDescription(trimToNull(request.shortDescription()));
		product.setDescription(trimToNull(request.description()));
		product.setImages(images);
		product.setSizes(nullToEmpty(request.sizes()).stream().map(ProductService::size).toList());
		product.setPricing(request.actualPrice(), request.discountPercent());
		product.setStock(request.stock());
		product.setTags(normalizeTags(request.tags()));
		product.setDraft(request.draft());
	}

	/** The same rules (and messages) the add-product page checks before submitting. */
	private static void validateForPublishing(ProductRequest request) {
		int shortDescriptionLength = request.shortDescription() == null ? 0 : request.shortDescription().trim().length();
		if (isBlank(request.name())) {
			throw ApiException.badRequest("enter product name");
		}
		if (shortDescriptionLength < 10 || shortDescriptionLength > 100) {
			throw ApiException.badRequest("short description must be between 10 to 100 letters long");
		}
		if (isBlank(request.description())) {
			throw ApiException.badRequest("enter detail description about the product");
		}
		if (nullToEmpty(request.images()).isEmpty()) {
			throw ApiException.badRequest("upload at least one product image");
		}
		if (nullToEmpty(request.sizes()).isEmpty()) {
			throw ApiException.badRequest("select at least one size");
		}
		if (request.actualPrice() == null || request.discountPercent() == null) {
			throw ApiException.badRequest("you must add pricings");
		}
		if (request.stock() == null || request.stock() < MIN_STOCK_TO_PUBLISH) {
			throw ApiException.badRequest("you should have at least 20 items in stock");
		}
		if (normalizeTags(request.tags()).isEmpty()) {
			throw ApiException.badRequest("enter few tags to help ranking your product in search");
		}
		if (!request.termsAccepted()) {
			throw ApiException.badRequest("you must agree to our terms and conditions");
		}
	}

	private Product findPublished(UUID id) {
		return products.findByIdAndDraftFalse(id).orElseThrow(() -> ApiException.notFound("product not found"));
	}

	private Product findOwned(UUID sellerId, UUID id) {
		// someone else's product looks exactly like a missing one
		return products.findByIdAndSellerId(id, sellerId)
			.orElseThrow(() -> ApiException.notFound("product not found"));
	}

	private static List<ProductResponse> toResponses(List<Product> list) {
		return list.stream().map(ProductResponse::from).toList();
	}

	private static Pageable firstPage() {
		return PageRequest.of(0, MAX_RESULTS);
	}

	static List<String> normalizeTags(List<String> tags) {
		Set<String> unique = new LinkedHashSet<>();
		nullToEmpty(tags).stream()
			.filter(Objects::nonNull)
			.map(ProductService::normalizeTag)
			.filter(tag -> !tag.isEmpty())
			.forEach(unique::add);
		if (unique.stream().anyMatch(tag -> tag.length() > 40)) {
			throw ApiException.badRequest("each tag can be at most 40 letters long");
		}
		return List.copyOf(unique);
	}

	/** Parses an API size code ("xs" ... "xxxl"); unknown codes are a 400 for the client. */
	public static ProductSize size(String code) {
		return ProductSize.parse(code).orElseThrow(() -> ApiException.badRequest("unknown size: " + code));
	}

	private static String normalizeTag(String tag) {
		return tag == null ? "" : tag.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}

	private static <T> List<T> nullToEmpty(List<T> list) {
		return list == null ? List.of() : list;
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private static String trimToNull(String value) {
		return isBlank(value) ? null : value.trim();
	}

}
