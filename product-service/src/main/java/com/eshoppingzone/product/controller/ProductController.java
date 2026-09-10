package com.eshoppingzone.product.controller;

import com.eshoppingzone.product.dto.*;
import com.eshoppingzone.product.security.JwtTokenValidator;
import com.eshoppingzone.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final JwtTokenValidator tokenValidator;

    public ProductController(ProductService productService, JwtTokenValidator tokenValidator) {
        this.productService = productService;
        this.tokenValidator = tokenValidator;
    }

    @PostMapping
    public ResponseEntity<ProductDto> addProduct(
            @Valid @RequestBody ProductCreateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long merchantId = extractProfileId(authHeader, request.getMerchantId());
        ProductDto created = productService.addProduct(request, merchantId);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<ProductDto>> getProductsByMerchant(@PathVariable("merchantId") Long merchantId) {
        return ResponseEntity.ok(productService.getProductsByMerchant(merchantId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductDto>> searchProducts(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "name", required = false) String name) {
        String searchTerm = query != null ? query : name;
        return ResponseEntity.ok(productService.searchProducts(searchTerm));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductDto>> getProductsByCategory(@PathVariable("category") String category) {
        return ResponseEntity.ok(productService.getProductsByCategory(category));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<ProductDto>> getProductsByType(@PathVariable("type") String type) {
        return ResponseEntity.ok(productService.getProductsByType(type));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(productService.getAllCategories());
    }

    @GetMapping("/subcategories")
    public ResponseEntity<List<String>> getSubcategories(@RequestParam(value = "category", required = false) String category) {
        return ResponseEntity.ok(productService.getSubcategoriesByCategory(category));
    }

    @GetMapping("/brands")
    public ResponseEntity<List<String>> getAllBrands() {
        return ResponseEntity.ok(productService.getAllBrands());
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<ProductDto>> getAllProductsForAdmin() {
        return ResponseEntity.ok(productService.getAllProductsForAdmin());
    }

    @PutMapping("/{id}/approval")
    public ResponseEntity<ProductDto> updateApprovalStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            @RequestParam(value = "reason", required = false) String reason) {
        return ResponseEntity.ok(productService.updateApprovalStatus(id, status, reason));
    }

    @GetMapping("/types")
    public ResponseEntity<List<String>> getAllProductTypes() {
        return ResponseEntity.ok(productService.getAllProductTypes());
    }

    @GetMapping("/manufacturers")
    public ResponseEntity<List<String>> getAllManufacturers() {
        return ResponseEntity.ok(productService.getAllManufacturers());
    }

    @GetMapping("/filter")
    public ResponseEntity<List<ProductDto>> filterProducts(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "manufacturer", required = false) String manufacturer,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "inStock", required = false) Boolean inStock,
            @RequestParam(value = "sortBy", required = false) String sortBy) {
        return ResponseEntity.ok(productService.filterProducts(category, type, manufacturer, minPrice, maxPrice, inStock, sortBy));
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<ProductAvailabilityDto> checkAvailability(@PathVariable("id") Long id) {
        return ResponseEntity.ok(productService.checkAvailability(id));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ProductReviewDto>> getReviews(@PathVariable("id") Long id) {
        return ResponseEntity.ok(productService.getProductReviews(id));
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ProductReviewDto> addReview(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreateReviewRequest request) {
        ProductReviewDto created = productService.addProductReview(id, request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/reviews/{reviewId}")
    public ResponseEntity<ProductReviewDto> updateReview(
            @PathVariable("id") Long id,
            @PathVariable("reviewId") Long reviewId,
            @Valid @RequestBody UpdateReviewRequest request) {
        return ResponseEntity.ok(productService.updateProductReview(id, reviewId, request));
    }

    @DeleteMapping("/{id}/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable("id") Long id,
            @PathVariable("reviewId") Long reviewId,
            @RequestParam("customerId") Long customerId) {
        productService.deleteProductReview(id, reviewId, customerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reports")
    public ResponseEntity<ProductReportDto> createReport(@Valid @RequestBody CreateReportRequest request) {
        ProductReportDto created = productService.createReport(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProductUpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long merchantId = extractProfileId(authHeader, request.getMerchantId());
        return ResponseEntity.ok(productService.updateProduct(id, request, merchantId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long merchantId = extractProfileId(authHeader, null);
        productService.deleteProduct(id, merchantId);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = {"/{id}/toggle-active", "/{id}/active"}, method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ProductDto> toggleProductActive(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "merchantId", required = false) Long merchantIdParam) {
        Long merchantId = extractProfileId(authHeader, merchantIdParam);
        return ResponseEntity.ok(productService.toggleProductActive(id, merchantId));
    }

    @RequestMapping(value = "/{id}/stock", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ProductDto> updateStock(
            @PathVariable("id") Long id,
            @Valid @RequestBody StockUpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "merchantId", required = false) Long merchantIdParam) {
        Long merchantId = extractProfileId(authHeader, merchantIdParam);
        if ("ADD".equalsIgnoreCase(request.getOperation())) {
            return ResponseEntity.ok(productService.addStock(id, request.getQuantity(), merchantId));
        }
        return ResponseEntity.ok(productService.updateStock(id, request.getQuantity(), merchantId));
    }

    @RequestMapping(value = "/{id}/add-stock", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ProductDto> addStock(
            @PathVariable("id") Long id,
            @RequestParam("quantity") Integer quantity,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "merchantId", required = false) Long merchantIdParam) {
        Long merchantId = extractProfileId(authHeader, merchantIdParam);
        return ResponseEntity.ok(productService.addStock(id, quantity, merchantId));
    }

    @RequestMapping(value = "/{id}/deduct-stock", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ProductDto> deductStock(
            @PathVariable("id") Long id,
            @RequestParam("quantity") Integer quantity) {
        return ResponseEntity.ok(productService.deductStock(id, quantity));
    }

    @RequestMapping(value = {"/{id}/out-of-stock", "/{id}/outOfStock"}, method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ProductDto> markOutOfStock(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "merchantId", required = false) Long merchantIdParam) {
        Long merchantId = extractProfileId(authHeader, merchantIdParam);
        return ResponseEntity.ok(productService.markOutOfStock(id, merchantId));
    }

    @RequestMapping(value = "/{id}/restock", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ProductDto> restock(
            @PathVariable("id") Long id,
            @RequestParam(value = "quantity", required = false, defaultValue = "50") Integer quantity,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "merchantId", required = false) Long merchantIdParam) {
        Long merchantId = extractProfileId(authHeader, merchantIdParam);
        return ResponseEntity.ok(productService.restock(id, quantity, merchantId));
    }

    @GetMapping("/merchant/{merchantId}/low-stock")
    public ResponseEntity<List<ProductDto>> getLowStockProducts(@PathVariable("merchantId") Long merchantId) {
        return ResponseEntity.ok(productService.getLowStockProducts(merchantId));
    }

    @RequestMapping(value = "/{id}/discount", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ProductDto> setDiscount(
            @PathVariable("id") Long id,
            @RequestParam("discountPercent") Double discountPercent,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "merchantId", required = false) Long merchantIdParam) {
        Long merchantId = extractProfileId(authHeader, merchantIdParam);
        return ResponseEntity.ok(productService.setDiscount(id, discountPercent, merchantId));
    }

    @GetMapping("/{id}/variants")
    public ResponseEntity<List<ProductVariantDto>> getVariants(@PathVariable("id") Long productId) {
        return ResponseEntity.ok(productService.getVariants(productId));
    }

    @PostMapping("/{id}/variants")
    public ResponseEntity<ProductVariantDto> addVariant(
            @PathVariable("id") Long productId,
            @Valid @RequestBody VariantCreateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "merchantId", required = false) Long merchantIdParam) {
        Long merchantId = extractProfileId(authHeader, merchantIdParam);
        ProductVariantDto created = productService.addVariant(productId, request, merchantId);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/variants/{variantId}")
    public ResponseEntity<ProductVariantDto> updateVariant(
            @PathVariable("id") Long productId,
            @PathVariable("variantId") Long variantId,
            @Valid @RequestBody VariantCreateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "merchantId", required = false) Long merchantIdParam) {
        Long merchantId = extractProfileId(authHeader, merchantIdParam);
        return ResponseEntity.ok(productService.updateVariant(productId, variantId, request, merchantId));
    }

    @DeleteMapping("/{id}/variants/{variantId}")
    public ResponseEntity<Void> deleteVariant(
            @PathVariable("id") Long productId,
            @PathVariable("variantId") Long variantId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "merchantId", required = false) Long merchantIdParam) {
        Long merchantId = extractProfileId(authHeader, merchantIdParam);
        productService.deleteVariant(productId, variantId, merchantId);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/{id}/image", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ProductDto> uploadImage(
            @PathVariable("id") Long id,
            @RequestParam("imageUrl") String imageUrl,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "merchantId", required = false) Long merchantIdParam) {
        Long merchantId = extractProfileId(authHeader, merchantIdParam);
        return ResponseEntity.ok(productService.uploadImage(id, imageUrl, merchantId));
    }

    private Long extractProfileId(String authHeader, Long fallbackId) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (tokenValidator.validateToken(token)) {
                Long profileId = tokenValidator.getProfileIdFromToken(token);
                if (profileId != null) {
                    return profileId;
                }
            }
        }
        return fallbackId;
    }
}
