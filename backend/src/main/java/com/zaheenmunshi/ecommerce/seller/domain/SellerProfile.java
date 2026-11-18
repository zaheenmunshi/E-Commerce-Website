package com.zaheenmunshi.ecommerce.seller.domain;

import com.zaheenmunshi.ecommerce.common.domain.BaseEntity;
import com.zaheenmunshi.ecommerce.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** The business details a customer submits to become a seller. */
@Entity
@Table(name = "seller_profiles")
public class SellerProfile extends BaseEntity {

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(name = "business_name", nullable = false, length = 120)
	private String businessName;

	@Column(nullable = false, length = 500)
	private String address;

	@Column(nullable = false, length = 2000)
	private String about;

	@Column(nullable = false, length = 15)
	private String phone;

	protected SellerProfile() {
	}

	public SellerProfile(User user, String businessName, String address, String about, String phone) {
		this.user = user;
		this.businessName = businessName;
		this.address = address;
		this.about = about;
		this.phone = phone;
	}

	public User getUser() {
		return user;
	}

	public String getBusinessName() {
		return businessName;
	}

	public String getAddress() {
		return address;
	}

	public String getAbout() {
		return about;
	}

	public String getPhone() {
		return phone;
	}

}
