package com.zaheenmunshi.ecommerce.product.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {

	Optional<Product> findByIdAndDraftFalse(UUID id);

	Optional<Product> findByIdAndSellerId(UUID id, UUID sellerId);

	List<Product> findBySellerIdOrderByCreatedAtDesc(UUID sellerId);

	@Query("""
			select distinct p from Product p join p.tags t
			where p.draft = false and t = :tag
			order by p.createdAt desc""")
	List<Product> findPublishedByTag(@Param("tag") String tag, Pageable page);

	/** Exact tag match, or the words appear in the name / short description. */
	@Query("""
			select distinct p from Product p left join p.tags t
			where p.draft = false
			  and (t = :term or lower(p.name) like :pattern or lower(p.shortDescription) like :pattern)
			order by p.createdAt desc""")
	List<Product> searchPublished(@Param("term") String term, @Param("pattern") String pattern, Pageable page);

	@Query("""
			select distinct p from Product p join p.tags t
			where p.draft = false and p.id <> :excludeId and t in :tags
			order by p.createdAt desc""")
	List<Product> findPublishedSharingTags(@Param("excludeId") UUID excludeId, @Param("tags") Collection<String> tags,
			Pageable page);

}
