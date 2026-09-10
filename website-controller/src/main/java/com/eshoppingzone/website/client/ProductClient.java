package com.eshoppingzone.website.client;

import com.eshoppingzone.website.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "product-service", url = "${services.product-service.url:http://localhost:8082}")
public interface ProductClient {

    @GetMapping("/api/products")
    List<ProductDto> getAllProducts();

    @GetMapping("/api/products/{id}")
    ProductDto getProductById(@PathVariable("id") Long id);

    @GetMapping("/api/products/merchant/{merchantId}")
    List<ProductDto> getProductsByMerchant(@PathVariable("merchantId") Long merchantId);

    @GetMapping("/api/products/search")
    List<ProductDto> searchProducts(@RequestParam(value = "query", required = false) String query);

    @GetMapping("/api/products/category/{category}")
    List<ProductDto> getProductsByCategory(@PathVariable("category") String category);

    @GetMapping("/api/products/type/{type}")
    List<ProductDto> getProductsByType(@PathVariable("type") String type);

    @GetMapping("/api/products/categories")
    List<String> getAllCategories();

    @GetMapping("/api/products/types")
    List<String> getAllProductTypes();

    @GetMapping("/api/products/manufacturers")
    List<String> getAllManufacturers();

    @GetMapping("/api/products/brands")
    List<String> getAllBrands();

    @GetMapping("/api/products/subcategories")
    List<String> getSubcategories(@RequestParam(value = "category", required = false) String category);

    @GetMapping("/api/products/admin/all")
    List<ProductDto> getAllProductsForAdmin();

    @PutMapping("/api/products/{id}/approval")
    ProductDto updateApprovalStatus(@PathVariable("id") Long id, @RequestParam("status") String status, @RequestParam(value = "reason", required = false) String reason);

    @GetMapping("/api/products/filter")
    List<ProductDto> filterProducts(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "manufacturer", required = false) String manufacturer,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "inStock", required = false) Boolean inStock,
            @RequestParam(value = "sortBy", required = false) String sortBy);

    @GetMapping("/api/products/{id}/availability")
    ProductAvailabilityDto checkAvailability(@PathVariable("id") Long id);

    @GetMapping("/api/products/{id}/reviews")
    List<ProductReviewDto> getReviews(@PathVariable("id") Long id);

    @PostMapping("/api/products/{id}/reviews")
    ProductReviewDto addReview(@PathVariable("id") Long id, @RequestBody CreateReviewRequest request);

    @PutMapping("/api/products/{id}/reviews/{reviewId}")
    ProductReviewDto updateReview(@PathVariable("id") Long id, @PathVariable("reviewId") Long reviewId, @RequestBody UpdateReviewRequest request);

    @DeleteMapping("/api/products/{id}/reviews/{reviewId}")
    void deleteReview(@PathVariable("id") Long id, @PathVariable("reviewId") Long reviewId, @RequestParam("customerId") Long customerId);

    @PostMapping("/api/products/reports")
    ProductReportDto createReport(@RequestBody CreateReportRequest request);

    @PostMapping("/api/products")
    ProductDto addProduct(@RequestBody ProductCreateRequest request);

    @PutMapping("/api/products/{id}")
    ProductDto updateProduct(@PathVariable("id") Long id, @RequestBody ProductUpdateRequest request);

    @DeleteMapping("/api/products/{id}")
    void deleteProduct(@PathVariable("id") Long id);

    @PostMapping("/api/products/{id}/toggle-active")
    ProductDto toggleProductActive(@PathVariable("id") Long id, @RequestParam(value = "merchantId", required = false) Long merchantId);

    @PostMapping("/api/products/{id}/stock")
    ProductDto updateStock(@PathVariable("id") Long id, @RequestBody StockUpdateRequest request, @RequestParam(value = "merchantId", required = false) Long merchantId);

    @PostMapping("/api/products/{id}/add-stock")
    ProductDto addStock(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity, @RequestParam(value = "merchantId", required = false) Long merchantId);

    @PostMapping("/api/products/{id}/out-of-stock")
    ProductDto markOutOfStock(@PathVariable("id") Long id, @RequestParam(value = "merchantId", required = false) Long merchantId);

    @PostMapping("/api/products/{id}/restock")
    ProductDto restock(@PathVariable("id") Long id, @RequestParam(value = "quantity", required = false) Integer quantity, @RequestParam(value = "merchantId", required = false) Long merchantId);

    @GetMapping("/api/products/merchant/{merchantId}/low-stock")
    List<ProductDto> getLowStockProducts(@PathVariable("merchantId") Long merchantId);

    @PostMapping("/api/products/{id}/discount")
    ProductDto setDiscount(@PathVariable("id") Long id, @RequestParam("discountPercent") Double discountPercent, @RequestParam(value = "merchantId", required = false) Long merchantId);

    @GetMapping("/api/products/{id}/variants")
    List<ProductVariantDto> getVariants(@PathVariable("id") Long productId);

    @PostMapping("/api/products/{id}/variants")
    ProductVariantDto addVariant(@PathVariable("id") Long productId, @RequestBody VariantCreateRequest request, @RequestParam(value = "merchantId", required = false) Long merchantId);

    @PutMapping("/api/products/{id}/variants/{variantId}")
    ProductVariantDto updateVariant(@PathVariable("id") Long productId, @PathVariable("variantId") Long variantId, @RequestBody VariantCreateRequest request, @RequestParam(value = "merchantId", required = false) Long merchantId);

    @DeleteMapping("/api/products/{id}/variants/{variantId}")
    void deleteVariant(@PathVariable("id") Long productId, @PathVariable("variantId") Long variantId, @RequestParam(value = "merchantId", required = false) Long merchantId);

    @PostMapping("/api/products/{id}/image")
    ProductDto uploadImage(@PathVariable("id") Long id, @RequestParam("imageUrl") String imageUrl, @RequestParam(value = "merchantId", required = false) Long merchantId);
}
