package com.eshoppingzone.product.service;

import com.eshoppingzone.product.dto.*;

import java.util.List;

public interface ProductService {

    ProductDto addProduct(ProductCreateRequest request);

    ProductDto addProduct(ProductCreateRequest request, Long merchantId);

    List<ProductDto> getAllProducts();

    ProductDto getProductById(Long id);

    List<ProductDto> getProductsByMerchant(Long merchantId);

    List<ProductDto> searchProducts(String name);

    List<ProductDto> getProductsByCategory(String category);

    List<ProductDto> getProductsByType(String type);

    List<String> getAllCategories();

    List<String> getAllProductTypes();

    ProductDto updateProduct(Long id, ProductUpdateRequest request);

    ProductDto updateProduct(Long id, ProductUpdateRequest request, Long authenticatedMerchantId);

    void deleteProduct(Long id);

    void deleteProduct(Long id, Long authenticatedMerchantId);

    List<ProductDto> filterProducts(String category, String type, String manufacturer, Double minPrice, Double maxPrice, Boolean inStock, String sortBy);

    ProductAvailabilityDto checkAvailability(Long productId);

    List<String> getAllManufacturers();

    List<ProductReviewDto> getProductReviews(Long productId);

    ProductReviewDto addProductReview(Long productId, CreateReviewRequest request);

    ProductReviewDto updateProductReview(Long productId, Long reviewId, UpdateReviewRequest request);

    void deleteProductReview(Long productId, Long reviewId, Long customerId);

    ProductReportDto createReport(CreateReportRequest request);

    ProductDto toggleProductActive(Long id, Long authenticatedMerchantId);

    ProductDto updateStock(Long id, Integer quantity, Long authenticatedMerchantId);

    ProductDto addStock(Long id, Integer quantity, Long authenticatedMerchantId);

    ProductDto deductStock(Long id, Integer quantity);

    ProductDto markOutOfStock(Long id, Long authenticatedMerchantId);

    ProductDto restock(Long id, Integer quantity, Long authenticatedMerchantId);

    List<ProductDto> getLowStockProducts(Long merchantId);

    ProductDto setDiscount(Long id, Double discountPercent, Long authenticatedMerchantId);

    List<ProductVariantDto> getVariants(Long productId);

    ProductVariantDto addVariant(Long productId, VariantCreateRequest request, Long authenticatedMerchantId);

    ProductVariantDto updateVariant(Long productId, Long variantId, VariantCreateRequest request, Long authenticatedMerchantId);

    void deleteVariant(Long productId, Long variantId, Long authenticatedMerchantId);

    ProductDto uploadImage(Long id, String imageUrl, Long authenticatedMerchantId);

    ProductDto updateApprovalStatus(Long id, String status, String reason);

    List<ProductDto> getAllProductsForAdmin();

    List<String> getAllBrands();

    List<String> getSubcategoriesByCategory(String category);
}
