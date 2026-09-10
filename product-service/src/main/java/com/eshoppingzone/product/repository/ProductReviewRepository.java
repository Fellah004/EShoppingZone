package com.eshoppingzone.product.repository;

import com.eshoppingzone.product.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    List<ProductReview> findByProductIdOrderByCreatedAtDesc(Long productId);
    Optional<ProductReview> findByReviewIdAndCustomerId(Long reviewId, Long customerId);
    List<ProductReview> findByProductId(Long productId);
}
