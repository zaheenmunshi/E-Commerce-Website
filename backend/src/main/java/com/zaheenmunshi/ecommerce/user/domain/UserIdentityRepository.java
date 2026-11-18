package com.zaheenmunshi.ecommerce.user.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {

	@EntityGraph(attributePaths = "user")
	Optional<UserIdentity> findByProviderAndSubject(String provider, String subject);

}
