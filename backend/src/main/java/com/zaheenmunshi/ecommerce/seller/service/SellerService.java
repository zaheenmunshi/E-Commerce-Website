package com.zaheenmunshi.ecommerce.seller.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zaheenmunshi.ecommerce.common.error.ApiException;
import com.zaheenmunshi.ecommerce.seller.domain.SellerProfile;
import com.zaheenmunshi.ecommerce.seller.domain.SellerProfileRepository;
import com.zaheenmunshi.ecommerce.seller.dto.SellerApplicationRequest;
import com.zaheenmunshi.ecommerce.user.domain.User;
import com.zaheenmunshi.ecommerce.user.domain.UserRepository;

@Service
public class SellerService {

	private final UserRepository users;

	private final SellerProfileRepository profiles;

	public SellerService(UserRepository users, SellerProfileRepository profiles) {
		this.users = users;
		this.profiles = profiles;
	}

	@Transactional
	public User becomeSeller(UUID userId, SellerApplicationRequest request) {
		User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("account not found"));
		if (user.isSeller() || profiles.existsByUserId(userId)) {
			throw ApiException.conflict("you are already a seller");
		}
		profiles.save(new SellerProfile(user, request.businessName().trim(), request.address().trim(),
				request.about().trim(), request.phone()));
		user.promoteToSeller();
		return user;
	}

}
