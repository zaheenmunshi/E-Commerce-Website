package com.zaheenmunshi.ecommerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Pretty page URLs for the static frontend. The pages are plain HTML; each one loads its
 * TypeScript module, which reads ids from the URL and calls the API. "/" is served by
 * Spring Boot's welcome page (index.html).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/login").setViewName("forward:/login.html");
		registry.addViewController("/signup").setViewName("forward:/signup.html");
		registry.addViewController("/cart").setViewName("forward:/cart.html");
		registry.addViewController("/checkout").setViewName("forward:/checkout.html");
		registry.addViewController("/search").setViewName("forward:/search.html");
		registry.addViewController("/seller").setViewName("forward:/seller.html");
		registry.addViewController("/add-product").setViewName("forward:/addProduct.html");
		registry.addViewController("/add-product/{id}").setViewName("forward:/addProduct.html");
		registry.addViewController("/products/{id}").setViewName("forward:/product.html");
		var notFound = registry.addViewController("/404");
		notFound.setViewName("forward:/error/404.html");
		notFound.setStatusCode(HttpStatus.NOT_FOUND);
	}

}
