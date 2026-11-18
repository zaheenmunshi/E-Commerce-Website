package com.zaheenmunshi.ecommerce.product.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.zaheenmunshi.ecommerce.common.domain.BaseEntity;
import com.zaheenmunshi.ecommerce.user.domain.User;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A product listing. Drafts may be incomplete, so most columns are nullable; the rules
 * for publishing live in {@code ProductService}.
 */
@Entity
@Table(name = "products", indexes = @Index(name = "ix_products_seller", columnList = "seller_id"))
public class Product extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "seller_id", nullable = false)
	private User seller;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(name = "short_description", length = 100)
	private String shortDescription;

	@Column(length = 5000)
	private String description;

	/** Image paths in display order; the first one is the thumbnail. */
	@ElementCollection
	@CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
	@OrderColumn(name = "sort_order")
	@Column(name = "url", nullable = false, length = 500)
	private List<String> images = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "product_sizes", joinColumns = @JoinColumn(name = "product_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "size_code", nullable = false, length = 8)
	private Set<ProductSize> sizes = EnumSet.noneOf(ProductSize.class);

	@Column(name = "actual_price", precision = 10, scale = 2)
	private BigDecimal actualPrice;

	@Column(name = "discount_percent")
	private Integer discountPercent;

	/** Derived from actual price and discount, never taken from the client. */
	@Column(name = "selling_price", precision = 10, scale = 2)
	private BigDecimal sellingPrice;

	private Integer stock;

	/** Lower-case search tags in the order the seller typed them, e.g. "men", "tshirt". */
	@ElementCollection
	@CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"),
			indexes = @Index(name = "ix_product_tags_tag", columnList = "tag"))
	@OrderColumn(name = "sort_order")
	@Column(name = "tag", nullable = false, length = 40)
	private List<String> tags = new ArrayList<>();

	@Column(nullable = false)
	private boolean draft;

	@Version
	private long version;

	protected Product() {
	}

	public Product(User seller) {
		this.seller = seller;
	}

	public void setPricing(BigDecimal actualPrice, Integer discountPercent) {
		this.actualPrice = actualPrice;
		this.discountPercent = discountPercent;
		this.sellingPrice = (actualPrice == null || discountPercent == null) ? null
				: actualPrice.multiply(BigDecimal.valueOf(100L - discountPercent))
					.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
	}

	public void removeStock(int quantity) {
		stock = stock - quantity;
	}

	public User getSeller() {
		return seller;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getShortDescription() {
		return shortDescription;
	}

	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getImages() {
		return images;
	}

	public void setImages(Collection<String> images) {
		this.images.clear();
		this.images.addAll(images);
	}

	public Set<ProductSize> getSizes() {
		return sizes;
	}

	public void setSizes(Collection<ProductSize> sizes) {
		this.sizes.clear();
		this.sizes.addAll(sizes);
	}

	public BigDecimal getActualPrice() {
		return actualPrice;
	}

	public Integer getDiscountPercent() {
		return discountPercent;
	}

	public BigDecimal getSellingPrice() {
		return sellingPrice;
	}

	public Integer getStock() {
		return stock;
	}

	public void setStock(Integer stock) {
		this.stock = stock;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(Collection<String> tags) {
		this.tags.clear();
		this.tags.addAll(tags);
	}

	public boolean isDraft() {
		return draft;
	}

	public void setDraft(boolean draft) {
		this.draft = draft;
	}

}
