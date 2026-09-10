package com.eshoppingzone.product.repository;

import com.eshoppingzone.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByMerchantId(Long merchantId);

    List<Product> findByCategoryIgnoreCase(String category);

    List<Product> findByProductTypeIgnoreCase(String productType);

    List<Product> findByProductNameContainingIgnoreCase(String productName);

    @Query("SELECT DISTINCT p.category FROM Product p ORDER BY p.category")
    List<String> findDistinctCategories();

    @Query("SELECT DISTINCT p.productType FROM Product p ORDER BY p.productType")
    List<String> findDistinctProductTypes();

    @Query("SELECT DISTINCT p.manufacturer FROM Product p ORDER BY p.manufacturer")
    List<String> findDistinctManufacturers();

    List<Product> findByActiveTrue();

    @Query("SELECT p FROM Product p WHERE p.merchantId = :merchantId AND p.stockQuantity <= :threshold")
    List<Product> findLowStockByMerchantId(@org.springframework.data.repository.query.Param("merchantId") Long merchantId, @org.springframework.data.repository.query.Param("threshold") Integer threshold);
}
