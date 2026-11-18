package com.zaheenmunshi.ecommerce.seller.domain;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerProfileRepository extends JpaRepository<SellerProfile, UUID> {

	boolean existsByUserId(UUID userId);

}
