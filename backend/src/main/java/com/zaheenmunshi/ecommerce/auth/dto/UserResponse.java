package com.zaheenmunshi.ecommerce.auth.dto;

import java.util.UUID;

import com.zaheenmunshi.ecommerce.security.AppUserPrincipal;
import com.zaheenmunshi.ecommerce.user.domain.User;

public record UserResponse(UUID id, String name, String email, boolean seller) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.isSeller());
	}

	public static UserResponse from(AppUserPrincipal principal) {
		return new UserResponse(principal.getId(), principal.getName(), principal.getUsername(), principal.isSeller());
	}

}
