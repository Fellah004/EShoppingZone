package com.eshoppingzone.product.repository;

import com.eshoppingzone.product.entity.ProductReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductReportRepository extends JpaRepository<ProductReport, Long> {
    List<ProductReport> findByTargetTypeAndTargetId(String targetType, Long targetId);
    boolean existsByTargetTypeAndTargetIdAndCustomerId(String targetType, Long targetId, Long customerId);
}
