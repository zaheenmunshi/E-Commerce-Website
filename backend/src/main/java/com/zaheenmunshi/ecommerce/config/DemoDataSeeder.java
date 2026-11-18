package com.zaheenmunshi.ecommerce.config;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.zaheenmunshi.ecommerce.product.domain.Product;
import com.zaheenmunshi.ecommerce.product.domain.ProductRepository;
import com.zaheenmunshi.ecommerce.product.domain.ProductSize;
import com.zaheenmunshi.ecommerce.seller.domain.SellerProfile;
import com.zaheenmunshi.ecommerce.seller.domain.SellerProfileRepository;
import com.zaheenmunshi.ecommerce.user.domain.User;
import com.zaheenmunshi.ecommerce.user.domain.UserRepository;

/**
 * Fills an empty catalogue with the pictures bundled in frontend/public/img so the shop
 * has something to show locally. Off by default; DEMO_DATA=true turns it on.
 */
@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

	private static final List<ProductSize> CLOTHING = List.of(ProductSize.S, ProductSize.M, ProductSize.L,
			ProductSize.XL);

	private static final List<ProductSize> SHOES = List.of(ProductSize.M, ProductSize.L, ProductSize.XL);

	private final UserRepository users;

	private final SellerProfileRepository sellerProfiles;

	private final ProductRepository products;

	private final PasswordEncoder passwordEncoder;

	private final AppProperties.DemoData settings;

	public DemoDataSeeder(UserRepository users, SellerProfileRepository sellerProfiles, ProductRepository products,
			PasswordEncoder passwordEncoder, AppProperties properties) {
		this.users = users;
		this.sellerProfiles = sellerProfiles;
		this.products = products;
		this.passwordEncoder = passwordEncoder;
		this.settings = properties.demoData();
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (products.count() > 0) {
			return;
		}
		User seller = users.findByEmail(settings.sellerEmail()).orElseGet(this::createSeller);
		products.saveAll(List.of(
				product(seller, "Olive Logo Tee", "Soft cotton crew-neck tee with a printed chest logo",
						List.of("/img/product-image-1.png", "/img/product-image-2.png", "/img/product-image-3.png",
								"/img/product-image-4.png"),
						CLOTHING, "39.99", 25, List.of("men", "tshirt", "casuals")),
				product(seller, "Teal Striped Tee", "Slim-fit tee with tonal chest stripes",
						List.of("/img/card2.png"), CLOTHING, "29.99", 20, List.of("men", "tshirt", "sports")),
				product(seller, "Beige Crew Sweatshirt", "Brushed fleece sweatshirt for cool evenings",
						List.of("/img/card3.png"), CLOTHING, "54.00", 30, List.of("men", "sweatshirt", "casuals")),
				product(seller, "Black Roll-Sleeve Shirt", "Lightweight cotton shirt with roll-up sleeves",
						List.of("/img/card5.png"), CLOTHING, "44.50", 15, List.of("men", "shirt", "casuals")),
				product(seller, "Black Casual Shirt", "Regular-fit button-down in breathable twill",
						List.of("/img/card6.png"), CLOTHING, "42.00", 10, List.of("men", "shirt")),
				product(seller, "Black Mandarin Shirt", "Band-collar shirt for a clean modern look",
						List.of("/img/card7.png"), CLOTHING, "48.00", 20, List.of("men", "shirt", "formals")),
				product(seller, "Denim Blue Oxford Shirt", "Classic oxford weave, smart or casual",
						List.of("/img/card8.png"), CLOTHING, "49.99", 15, List.of("men", "shirt", "formals")),
				product(seller, "Floral Midi Dress", "Flowing midi dress with a soft floral print",
						List.of("/img/card1.png"), CLOTHING, "69.00", 30, List.of("women", "dress", "casuals")),
				product(seller, "Fuchsia Wrap Dress", "Puff-sleeve wrap dress in a bold fuchsia",
						List.of("/img/card4.png"), CLOTHING, "74.00", 25, List.of("women", "dress", "formals")),
				product(seller, "Knit Runner Sneakers", "Breathable knit runners with a cushioned sole",
						List.of("/img/card9.png"), SHOES, "64.99", 20, List.of("men", "shoes", "sports")),
				product(seller, "Navy Slip-On Trainers", "Laceless trainers with a stretch collar",
						List.of("/img/card10.png"), SHOES, "58.00", 15, List.of("men", "shoes", "casuals")),
				product(seller, "Charcoal Knit Running Shoes", "Lightweight running shoes with grippy outsole",
						List.of("/img/card11.png"), SHOES, "72.00", 30, List.of("men", "shoes", "sports")),
				product(seller, "Classic Black Sneakers", "Everyday low-top sneakers with a contrast stripe",
						List.of("/img/card12.png"), SHOES, "55.00", 10, List.of("men", "shoes", "casuals"))));
		log.info("Demo catalogue created for seller {}", seller.getEmail());
	}

	private User createSeller() {
		String password = settings.sellerPassword();
		if (password == null || password.isBlank()) {
			// unusable random password: set DEMO_SELLER_PASSWORD to log in as the demo seller
			password = HexFormat.of().formatHex(new SecureRandom().generateSeed(24));
		}
		User seller = new User("Demo Seller", settings.sellerEmail(), passwordEncoder.encode(password), "9000000000",
				false);
		seller.promoteToSeller();
		users.save(seller);
		sellerProfiles.save(new SellerProfile(seller, "Clothing Demo Store", "Bhopal, India",
				"Sample listings created for local development.", "9000000000"));
		return seller;
	}

	private static Product product(User seller, String name, String shortDescription, List<String> images,
			List<ProductSize> sizes, String price, int discount, List<String> tags) {
		Product product = new Product(seller);
		product.setName(name);
		product.setShortDescription(shortDescription);
		product.setDescription(shortDescription + ". Easy care, true to size and made to last. "
				+ "Demo listing: replace it with your own products from the seller dashboard.");
		product.setImages(images);
		product.setSizes(sizes);
		product.setPricing(new BigDecimal(price), discount);
		product.setStock(50);
		product.setTags(tags);
		return product;
	}

}
