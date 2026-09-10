package com.eshoppingzone.product.service;

import com.eshoppingzone.product.dto.*;
import com.eshoppingzone.product.entity.Product;
import com.eshoppingzone.product.entity.ProductReport;
import com.eshoppingzone.product.entity.ProductReview;
import com.eshoppingzone.product.exception.ResourceNotFoundException;
import com.eshoppingzone.product.repository.ProductReportRepository;
import com.eshoppingzone.product.repository.ProductReviewRepository;
import com.eshoppingzone.product.repository.ProductRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductReviewRepository reviewRepository;
    private final ProductReportRepository reportRepository;
    private final com.eshoppingzone.product.repository.ProductVariantRepository variantRepository;

    public ProductServiceImpl(
            ProductRepository productRepository,
            ProductReviewRepository reviewRepository,
            ProductReportRepository reportRepository,
            com.eshoppingzone.product.repository.ProductVariantRepository variantRepository) {
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
        this.reportRepository = reportRepository;
        this.variantRepository = variantRepository;
    }

    @Override
    public ProductDto addProduct(ProductCreateRequest request) {
        return addProduct(request, request.getMerchantId());
    }

    @Override
    public ProductDto addProduct(ProductCreateRequest request, Long merchantId) {
        Product product = new Product();
        product.setMerchantId(merchantId != null ? merchantId : request.getMerchantId());
        product.setProductType(request.getProductType());
        product.setProductName(request.getProductName());
        product.setCategory(request.getCategory());
        product.setManufacturer(request.getManufacturer());
        product.setImage(request.getImage());
        product.setPrice(request.getPrice());
        product.setDescription(request.getDescription());
        product.setSpecification(request.getSpecification());
        if (request.getStockQuantity() != null) {
            product.setStockQuantity(Math.max(0, request.getStockQuantity()));
            product.setInStock(request.getStockQuantity() > 0);
        } else {
            product.setStockQuantity(100);
            product.setInStock(true);
        }
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        } else {
            product.setActive(true);
        }
        if (request.getDiscountPercent() != null) {
            double disc = Math.max(0.0, Math.min(100.0, request.getDiscountPercent()));
            product.setDiscountPercent(disc);
        } else {
            product.setDiscountPercent(0.0);
        }
        if (request.getLowStockThreshold() != null) {
            product.setLowStockThreshold(Math.max(1, request.getLowStockThreshold()));
        } else {
            product.setLowStockThreshold(10);
        }

        Product saved = productRepository.save(product);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToDto(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getProductsByMerchant(Long merchantId) {
        if (merchantId == null) {
            return getAllProducts();
        }
        return productRepository.findByMerchantId(merchantId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> searchProducts(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getAllProducts();
        }
        return productRepository.findByProductNameContainingIgnoreCase(name.trim()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getProductsByCategory(String category) {
        return productRepository.findByCategoryIgnoreCase(category).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getProductsByType(String type) {
        return productRepository.findByProductTypeIgnoreCase(type).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return productRepository.findDistinctCategories();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllProductTypes() {
        return productRepository.findDistinctProductTypes();
    }

    @Override
    public ProductDto updateProduct(Long id, ProductUpdateRequest request) {
        return updateProduct(id, request, null);
    }

    @Override
    public ProductDto updateProduct(Long id, ProductUpdateRequest request, Long authenticatedMerchantId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Enforce Merchant ownership if authenticated merchant is provided
        if (authenticatedMerchantId != null && product.getMerchantId() != null && !product.getMerchantId().equals(authenticatedMerchantId)) {
            throw new AccessDeniedException("Access Denied: You do not own product #" + id + " and cannot modify it.");
        }

        if (request.getProductType() != null) product.setProductType(request.getProductType());
        if (request.getProductName() != null) product.setProductName(request.getProductName());
        if (request.getCategory() != null) product.setCategory(request.getCategory());
        if (request.getManufacturer() != null) product.setManufacturer(request.getManufacturer());
        if (request.getImage() != null) product.setImage(request.getImage());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getSpecification() != null) product.setSpecification(request.getSpecification());
        if (request.getStockQuantity() != null) {
            product.setStockQuantity(Math.max(0, request.getStockQuantity()));
            product.setInStock(product.getStockQuantity() > 0);
        }
        if (request.getInStock() != null) {
            product.setInStock(request.getInStock());
        }
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
        if (request.getDiscountPercent() != null) {
            product.setDiscountPercent(Math.max(0.0, Math.min(100.0, request.getDiscountPercent())));
        }
        if (request.getLowStockThreshold() != null) {
            product.setLowStockThreshold(Math.max(1, request.getLowStockThreshold()));
        }

        Product updated = productRepository.save(product);
        return mapToDto(updated);
    }

    @Override
    public void deleteProduct(Long id) {
        deleteProduct(id, null);
    }

    @Override
    public void deleteProduct(Long id, Long authenticatedMerchantId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Enforce Merchant ownership if authenticated merchant is provided
        if (authenticatedMerchantId != null && product.getMerchantId() != null && !product.getMerchantId().equals(authenticatedMerchantId)) {
            throw new AccessDeniedException("Access Denied: You do not own product #" + id + " and cannot delete it.");
        }

        productRepository.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> filterProducts(
            String category,
            String type,
            String manufacturer,
            Double minPrice,
            Double maxPrice,
            Boolean inStock,
            String sortBy) {
        List<Product> products = productRepository.findAll();

        return products.stream()
                .filter(p -> p.getActive() == null || Boolean.TRUE.equals(p.getActive()))
                .filter(p -> category == null || category.isBlank() || category.equalsIgnoreCase(p.getCategory()))
                .filter(p -> type == null || type.isBlank() || type.equalsIgnoreCase(p.getProductType()))
                .filter(p -> manufacturer == null || manufacturer.isBlank() || manufacturer.equalsIgnoreCase(p.getManufacturer()))
                .filter(p -> minPrice == null || p.getDiscountedPrice() >= minPrice)
                .filter(p -> maxPrice == null || p.getDiscountedPrice() <= maxPrice)
                .filter(p -> inStock == null || (inStock && Boolean.TRUE.equals(p.getInStock())))
                .sorted((p1, p2) -> {
                    if ("price_asc".equalsIgnoreCase(sortBy)) {
                        return Double.compare(p1.getDiscountedPrice(), p2.getDiscountedPrice());
                    } else if ("price_desc".equalsIgnoreCase(sortBy)) {
                        return Double.compare(p2.getDiscountedPrice(), p1.getDiscountedPrice());
                    } else if ("name_asc".equalsIgnoreCase(sortBy)) {
                        return p1.getProductName().compareToIgnoreCase(p2.getProductName());
                    } else if ("name_desc".equalsIgnoreCase(sortBy)) {
                        return p2.getProductName().compareToIgnoreCase(p1.getProductName());
                    } else if ("rating_desc".equalsIgnoreCase(sortBy)) {
                        double r1 = p1.getRating() != null ? p1.getRating() : 0.0;
                        double r2 = p2.getRating() != null ? p2.getRating() : 0.0;
                        return Double.compare(r2, r1);
                    } else { // default: newest first
                        return Long.compare(p2.getProductId(), p1.getProductId());
                    }
                })
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductAvailabilityDto checkAvailability(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        boolean available = Boolean.TRUE.equals(product.getInStock()) && product.getStockQuantity() != null && product.getStockQuantity() > 0;
        String message = available ? "Product is in stock (" + product.getStockQuantity() + " units available)" : "Product is currently out of stock";

        return ProductAvailabilityDto.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .inStock(available)
                .stockQuantity(product.getStockQuantity() != null ? product.getStockQuantity() : 0)
                .message(message)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllManufacturers() {
        return productRepository.findDistinctManufacturers();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductReviewDto> getProductReviews(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::mapToReviewDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProductReviewDto addProductReview(Long productId, CreateReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        ProductReview review = ProductReview.builder()
                .productId(productId)
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .rating(request.getRating())
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .build();

        ProductReview saved = reviewRepository.save(review);
        recalculateProductRating(product);

        return mapToReviewDto(saved);
    }

    @Override
    public ProductReviewDto updateProductReview(Long productId, Long reviewId, UpdateReviewRequest request) {
        ProductReview review = reviewRepository.findByReviewIdAndCustomerId(reviewId, request.getCustomerId())
                .orElseThrow(() -> new AccessDeniedException("Review not found or you are not authorized to edit this review."));

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        ProductReview updated = reviewRepository.save(review);

        productRepository.findById(productId).ifPresent(this::recalculateProductRating);

        return mapToReviewDto(updated);
    }

    @Override
    public void deleteProductReview(Long productId, Long reviewId, Long customerId) {
        ProductReview review = reviewRepository.findByReviewIdAndCustomerId(reviewId, customerId)
                .orElseThrow(() -> new AccessDeniedException("Review not found or you are not authorized to delete this review."));

        reviewRepository.delete(review);
        productRepository.findById(productId).ifPresent(this::recalculateProductRating);
    }

    @Override
    public ProductReportDto createReport(CreateReportRequest request) {
        ProductReport report = ProductReport.builder()
                .targetType(request.getTargetType().toUpperCase())
                .targetId(request.getTargetId())
                .customerId(request.getCustomerId())
                .reason(request.getReason())
                .comments(request.getComments())
                .reportDate(LocalDateTime.now())
                .status("PENDING")
                .build();

        ProductReport saved = reportRepository.save(report);
        return ProductReportDto.builder()
                .reportId(saved.getReportId())
                .targetType(saved.getTargetType())
                .targetId(saved.getTargetId())
                .customerId(saved.getCustomerId())
                .reason(saved.getReason())
                .comments(saved.getComments())
                .reportDate(saved.getReportDate())
                .status(saved.getStatus())
                .build();
    }

    @Override
    public ProductDto toggleProductActive(Long id, Long authenticatedMerchantId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (authenticatedMerchantId != null && product.getMerchantId() != null && !product.getMerchantId().equals(authenticatedMerchantId)) {
            throw new AccessDeniedException("Access Denied: You do not own product #" + id);
        }

        boolean currentActive = product.getActive() != null ? product.getActive() : true;
        product.setActive(!currentActive);
        Product saved = productRepository.save(product);
        return mapToDto(saved);
    }

    @Override
    public ProductDto updateStock(Long id, Integer quantity, Long authenticatedMerchantId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (authenticatedMerchantId != null && product.getMerchantId() != null && !product.getMerchantId().equals(authenticatedMerchantId)) {
            throw new AccessDeniedException("Access Denied: You do not own product #" + id);
        }

        int newQty = Math.max(0, quantity != null ? quantity : 0);
        product.setStockQuantity(newQty);
        product.setInStock(newQty > 0);

        Product saved = productRepository.save(product);
        return mapToDto(saved);
    }

    @Override
    public ProductDto addStock(Long id, Integer quantity, Long authenticatedMerchantId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (authenticatedMerchantId != null && product.getMerchantId() != null && !product.getMerchantId().equals(authenticatedMerchantId)) {
            throw new AccessDeniedException("Access Denied: You do not own product #" + id);
        }

        int addAmount = Math.max(0, quantity != null ? quantity : 0);
        int currentQty = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        int newQty = currentQty + addAmount;
        product.setStockQuantity(newQty);
        product.setInStock(newQty > 0);

        Product saved = productRepository.save(product);
        return mapToDto(saved);
    }

    @Override
    public ProductDto deductStock(Long id, Integer quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        int deduct = (quantity != null && quantity > 0) ? quantity : 0;
        int currentQty = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        int newQty = Math.max(0, currentQty - deduct);
        product.setStockQuantity(newQty);
        product.setInStock(newQty > 0);

        Product saved = productRepository.save(product);
        return mapToDto(saved);
    }

    @Override
    public ProductDto markOutOfStock(Long id, Long authenticatedMerchantId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (authenticatedMerchantId != null && product.getMerchantId() != null && !product.getMerchantId().equals(authenticatedMerchantId)) {
            throw new AccessDeniedException("Access Denied: You do not own product #" + id);
        }

        product.setInStock(false);
        product.setStockQuantity(0);

        Product saved = productRepository.save(product);
        return mapToDto(saved);
    }

    @Override
    public ProductDto restock(Long id, Integer quantity, Long authenticatedMerchantId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (authenticatedMerchantId != null && product.getMerchantId() != null && !product.getMerchantId().equals(authenticatedMerchantId)) {
            throw new AccessDeniedException("Access Denied: You do not own product #" + id);
        }

        int restockAmount = (quantity != null && quantity > 0) ? quantity : 50;
        int currentQty = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        int newQty = currentQty + restockAmount;
        product.setStockQuantity(newQty);
        product.setInStock(true);

        Product saved = productRepository.save(product);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getLowStockProducts(Long merchantId) {
        if (merchantId == null) {
            return productRepository.findAll().stream()
                    .filter(Product::isLowStock)
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        }

        return productRepository.findByMerchantId(merchantId).stream()
                .filter(Product::isLowStock)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProductDto setDiscount(Long id, Double discountPercent, Long authenticatedMerchantId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (authenticatedMerchantId != null && product.getMerchantId() != null && !product.getMerchantId().equals(authenticatedMerchantId)) {
            throw new AccessDeniedException("Access Denied: You do not own product #" + id);
        }

        double disc = Math.max(0.0, Math.min(100.0, discountPercent != null ? discountPercent : 0.0));
        product.setDiscountPercent(disc);

        Product saved = productRepository.save(product);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantDto> getVariants(Long productId) {
        return variantRepository.findByProductId(productId).stream()
                .map(this::mapToVariantDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProductVariantDto addVariant(Long productId, VariantCreateRequest request, Long authenticatedMerchantId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (authenticatedMerchantId != null && product.getMerchantId() != null && !product.getMerchantId().equals(authenticatedMerchantId)) {
            throw new AccessDeniedException("Access Denied: You do not own product #" + productId);
        }

        com.eshoppingzone.product.entity.ProductVariant variant = com.eshoppingzone.product.entity.ProductVariant.builder()
                .productId(productId)
                .variantName(request.getVariantName())
                .variantValue(request.getVariantValue())
                .price(request.getPrice() != null ? request.getPrice() : product.getPrice())
                .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 10)
                .sku(request.getSku())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        com.eshoppingzone.product.entity.ProductVariant saved = variantRepository.save(variant);
        return mapToVariantDto(saved);
    }

    @Override
    public ProductVariantDto updateVariant(Long productId, Long variantId, VariantCreateRequest request, Long authenticatedMerchantId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (authenticatedMerchantId != null && product.getMerchantId() != null && !product.getMerchantId().equals(authenticatedMerchantId)) {
            throw new AccessDeniedException("Access Denied: You do not own product #" + productId);
        }

        com.eshoppingzone.product.entity.ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        if (!variant.getProductId().equals(productId)) {
            throw new IllegalArgumentException("Variant #" + variantId + " does not belong to product #" + productId);
        }

        if (request.getVariantName() != null) variant.setVariantName(request.getVariantName());
        if (request.getVariantValue() != null) variant.setVariantValue(request.getVariantValue());
        if (request.getPrice() != null) variant.setPrice(request.getPrice());
        if (request.getStockQuantity() != null) variant.setStockQuantity(Math.max(0, request.getStockQuantity()));
        if (request.getSku() != null) variant.setSku(request.getSku());
        if (request.getActive() != null) variant.setActive(request.getActive());

        com.eshoppingzone.product.entity.ProductVariant saved = variantRepository.save(variant);
        return mapToVariantDto(saved);
    }

    @Override
    public void deleteVariant(Long productId, Long variantId, Long authenticatedMerchantId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (authenticatedMerchantId != null && product.getMerchantId() != null && !product.getMerchantId().equals(authenticatedMerchantId)) {
            throw new AccessDeniedException("Access Denied: You do not own product #" + productId);
        }

        com.eshoppingzone.product.entity.ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        if (!variant.getProductId().equals(productId)) {
            throw new IllegalArgumentException("Variant #" + variantId + " does not belong to product #" + productId);
        }

        variantRepository.delete(variant);
    }

    @Override
    public ProductDto uploadImage(Long id, String imageUrl, Long authenticatedMerchantId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (authenticatedMerchantId != null && product.getMerchantId() != null && !product.getMerchantId().equals(authenticatedMerchantId)) {
            throw new AccessDeniedException("Access Denied: You do not own product #" + id);
        }

        product.setImage(imageUrl);
        Product saved = productRepository.save(product);
        return mapToDto(saved);
    }

    private void recalculateProductRating(Product product) {
        List<ProductReview> reviews = reviewRepository.findByProductId(product.getProductId());
        if (reviews.isEmpty()) {
            product.setRating(null);
        } else {
            double avg = reviews.stream().mapToDouble(ProductReview::getRating).average().orElse(0.0);
            product.setRating(Math.round(avg * 10.0) / 10.0);
        }
        productRepository.save(product);
    }

    private ProductReviewDto mapToReviewDto(ProductReview review) {
        return ProductReviewDto.builder()
                .reviewId(review.getReviewId())
                .productId(review.getProductId())
                .customerId(review.getCustomerId())
                .customerName(review.getCustomerName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private ProductVariantDto mapToVariantDto(com.eshoppingzone.product.entity.ProductVariant variant) {
        return ProductVariantDto.builder()
                .variantId(variant.getVariantId())
                .productId(variant.getProductId())
                .variantName(variant.getVariantName())
                .variantValue(variant.getVariantValue())
                .price(variant.getPrice())
                .stockQuantity(variant.getStockQuantity())
                .sku(variant.getSku())
                .active(variant.getActive())
                .build();
    }

    @Override
    @Transactional
    public ProductDto updateApprovalStatus(Long id, String status, String reason) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setApprovalStatus(status);
        if (reason != null && !reason.isBlank()) {
            product.setRejectionReason(reason);
        }
        if ("APPROVED".equalsIgnoreCase(status)) {
            product.setActive(true);
        } else if ("REJECTED".equalsIgnoreCase(status) || "PROHIBITED".equalsIgnoreCase(status)) {
            product.setActive(false);
        }
        Product saved = productRepository.save(product);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getAllProductsForAdmin() {
        return productRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllBrands() {
        return productRepository.findDistinctManufacturers();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSubcategoriesByCategory(String category) {
        if (category == null || category.isBlank()) {
            return productRepository.findDistinctProductTypes();
        }
        return productRepository.findByCategoryIgnoreCase(category).stream()
                .map(Product::getProductType)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private ProductDto mapToDto(Product product) {
        List<ProductVariantDto> variants = new java.util.ArrayList<>();
        try {
            if (product.getProductId() != null) {
                variants = variantRepository.findByProductId(product.getProductId()).stream()
                        .map(this::mapToVariantDto)
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) {
        }

        ProductDto dto = new ProductDto(
                product.getProductId(),
                product.getMerchantId(),
                product.getProductType(),
                product.getProductName(),
                product.getCategory(),
                product.getManufacturer(),
                product.getImage(),
                product.getPrice(),
                product.getDescription(),
                product.getSpecification(),
                product.getRating(),
                product.getReview(),
                product.getStockQuantity() != null ? product.getStockQuantity() : 100,
                product.getInStock() != null ? product.getInStock() : true,
                product.getActive() != null ? product.getActive() : true,
                product.getDiscountPercent() != null ? product.getDiscountPercent() : 0.0,
                product.getDiscountedPrice(),
                product.getLowStockThreshold() != null ? product.getLowStockThreshold() : 10
        );
        dto.setApprovalStatus(product.getApprovalStatus() != null ? product.getApprovalStatus() : "APPROVED");
        dto.setRejectionReason(product.getRejectionReason());
        dto.setBrand(product.getBrand() != null ? product.getBrand() : product.getManufacturer());
        dto.setSubcategory(product.getSubcategory() != null ? product.getSubcategory() : product.getProductType());
        dto.setVariants(variants);
        return dto;
    }
}
