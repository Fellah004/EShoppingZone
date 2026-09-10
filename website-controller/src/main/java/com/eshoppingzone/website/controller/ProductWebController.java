package com.eshoppingzone.website.controller;

import com.eshoppingzone.website.client.CartClient;
import com.eshoppingzone.website.client.ProductClient;
import com.eshoppingzone.website.dto.CartDto;
import com.eshoppingzone.website.dto.ProductDto;
import com.eshoppingzone.website.session.SessionAuthHelper;
import com.eshoppingzone.website.session.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductWebController {

    private final ProductClient productClient;
    private final CartClient cartClient;
    private final SessionAuthHelper authHelper;

    public ProductWebController(ProductClient productClient, CartClient cartClient, SessionAuthHelper authHelper) {
        this.productClient = productClient;
        this.cartClient = cartClient;
        this.authHelper = authHelper;
    }

    private void populateCommonAttributes(HttpSession session, Model model) {
        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        int cartCount = 0;
        if (currentUser != null && currentUser.isLoggedIn()) {
            try {
                CartDto cart = cartClient.getCartByCustomerId(currentUser.getProfileId());
                if (cart != null) {
                    cartCount = cart.getTotalItemCount();
                }
            } catch (Exception ignored) {
            }
        }
        model.addAttribute("cartCount", cartCount);

        try {
            model.addAttribute("categories", productClient.getAllCategories());
            model.addAttribute("types", productClient.getAllProductTypes());
            model.addAttribute("manufacturers", productClient.getAllManufacturers());
        } catch (Exception ignored) {
        }
    }

    @GetMapping
    public String listProducts(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "manufacturer", required = false) String manufacturer,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "inStock", required = false) Boolean inStock,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        populateCommonAttributes(session, model);

        List<ProductDto> allProducts = new ArrayList<>();
        StringBuilder baseUrlBuilder = new StringBuilder("/products?");
        if (category != null && !category.isBlank()) {
            baseUrlBuilder.append("category=").append(java.net.URLEncoder.encode(category, java.nio.charset.StandardCharsets.UTF_8)).append("&");
            model.addAttribute("selectedCategory", category);
        }
        if (type != null && !type.isBlank()) {
            baseUrlBuilder.append("type=").append(java.net.URLEncoder.encode(type, java.nio.charset.StandardCharsets.UTF_8)).append("&");
            model.addAttribute("selectedType", type);
        }
        if (manufacturer != null && !manufacturer.isBlank()) {
            baseUrlBuilder.append("manufacturer=").append(java.net.URLEncoder.encode(manufacturer, java.nio.charset.StandardCharsets.UTF_8)).append("&");
            model.addAttribute("selectedManufacturer", manufacturer);
        }
        if (minPrice != null) {
            baseUrlBuilder.append("minPrice=").append(minPrice).append("&");
            model.addAttribute("selectedMinPrice", minPrice);
        }
        if (maxPrice != null) {
            baseUrlBuilder.append("maxPrice=").append(maxPrice).append("&");
            model.addAttribute("selectedMaxPrice", maxPrice);
        }
        if (inStock != null) {
            baseUrlBuilder.append("inStock=").append(inStock).append("&");
            model.addAttribute("selectedInStock", inStock);
        }
        if (sortBy != null && !sortBy.isBlank()) {
            baseUrlBuilder.append("sortBy=").append(java.net.URLEncoder.encode(sortBy, java.nio.charset.StandardCharsets.UTF_8)).append("&");
            model.addAttribute("selectedSortBy", sortBy);
        }

        String baseUrl = baseUrlBuilder.toString();
        if (baseUrl.endsWith("&") || baseUrl.endsWith("?")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        try {
            allProducts = productClient.filterProducts(category, type, manufacturer, minPrice, maxPrice, inStock, sortBy);
        } catch (Exception ex) {
            model.addAttribute("error", "Unable to load products. Please check Product Service.");
        }

        com.eshoppingzone.website.dto.PagedResult<ProductDto> pageData = com.eshoppingzone.website.dto.PagedResult.from(allProducts, page, size);
        model.addAttribute("products", pageData.getContent());
        model.addAttribute("pageData", pageData);
        model.addAttribute("baseUrl", baseUrl.isEmpty() ? "/products" : baseUrl);
        return "products/list";
    }

    @GetMapping("/category/{category}")
    public String productsByCategory(
            @PathVariable("category") String category,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        return listProducts(category, null, null, null, null, null, null, page, size, session, model);
    }

    @GetMapping("/type/{type}")
    public String productsByType(
            @PathVariable("type") String type,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        return listProducts(null, type, null, null, null, null, null, page, size, session, model);
    }

    @GetMapping("/search")
    public String searchProducts(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        populateCommonAttributes(session, model);

        List<ProductDto> allProducts = new ArrayList<>();
        try {
            allProducts = productClient.searchProducts(query);
        } catch (Exception ex) {
            model.addAttribute("error", "Unable to complete search.");
        }

        com.eshoppingzone.website.dto.PagedResult<ProductDto> pageData = com.eshoppingzone.website.dto.PagedResult.from(allProducts, page, size);
        String baseUrl = "/products/search" + (query != null && !query.isBlank() ? "?query=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8) : "");

        model.addAttribute("searchQuery", query);
        model.addAttribute("products", pageData.getContent());
        model.addAttribute("pageData", pageData);
        model.addAttribute("resultCount", pageData.getTotalElements());
        model.addAttribute("baseUrl", baseUrl);
        return "products/search";
    }

    @GetMapping("/{id}")
    public String productDetails(
            @PathVariable("id") Long id,
            HttpSession session,
            Model model) {
        populateCommonAttributes(session, model);

        try {
            ProductDto product = productClient.getProductById(id);
            model.addAttribute("product", product);

            // Fetch availability
            try {
                com.eshoppingzone.website.dto.ProductAvailabilityDto availability = productClient.checkAvailability(id);
                model.addAttribute("availability", availability);
            } catch (Exception ignored) {
            }

            // Fetch reviews
            try {
                java.util.List<com.eshoppingzone.website.dto.ProductReviewDto> reviews = productClient.getReviews(id);
                model.addAttribute("reviews", reviews);
            } catch (Exception ignored) {
                model.addAttribute("reviews", java.util.Collections.emptyList());
            }

            // Related products in the same category
            if (product.getCategory() != null) {
                List<ProductDto> categoryProducts = productClient.getProductsByCategory(product.getCategory());
                List<ProductDto> related = categoryProducts.stream()
                        .filter(p -> !p.getProductId().equals(id))
                        .limit(4)
                        .toList();
                model.addAttribute("relatedProducts", related);
            }
        } catch (Exception ex) {
            model.addAttribute("error", "Product not found or service unavailable.");
            return "redirect:/products";
        }

        return "products/detail";
    }

    @PostMapping("/{id}/review")
    public String addReview(
            @PathVariable("id") Long productId,
            @RequestParam("rating") Double rating,
            @RequestParam(value = "comment", required = false) String comment,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/products/" + productId;
        }
        UserSession currentUser = authHelper.getCurrentUser(session);

        try {
            com.eshoppingzone.website.dto.CreateReviewRequest request = new com.eshoppingzone.website.dto.CreateReviewRequest(
                    currentUser.getProfileId(),
                    currentUser.getFullName() != null ? currentUser.getFullName() : "Customer",
                    rating,
                    comment
            );
            productClient.addReview(productId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Your review and rating have been posted!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to post review: " + ex.getMessage());
        }

        return "redirect:/products/" + productId;
    }

    @PostMapping("/{id}/review/{reviewId}/delete")
    public String deleteReview(
            @PathVariable("id") Long productId,
            @PathVariable("reviewId") Long reviewId,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/products/" + productId;
        }
        UserSession currentUser = authHelper.getCurrentUser(session);

        try {
            productClient.deleteReview(productId, reviewId, currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "Your review has been removed.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete review: " + ex.getMessage());
        }

        return "redirect:/products/" + productId;
    }

    @PostMapping("/{id}/report")
    public String submitReport(
            @PathVariable("id") Long productId,
            @RequestParam("targetType") String targetType,
            @RequestParam("targetId") Long targetId,
            @RequestParam("reason") String reason,
            @RequestParam(value = "comments", required = false) String comments,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/products/" + productId;
        }
        UserSession currentUser = authHelper.getCurrentUser(session);

        try {
            com.eshoppingzone.website.dto.CreateReportRequest request = new com.eshoppingzone.website.dto.CreateReportRequest(
                    targetType,
                    targetId,
                    currentUser.getProfileId(),
                    reason,
                    comments
            );
            productClient.createReport(request);
            redirectAttributes.addFlashAttribute("successMessage", "Thank you. Your report has been submitted for moderation review.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to submit report: " + ex.getMessage());
        }

        return "redirect:/products/" + productId;
    }

    @GetMapping("/{id}/availability")
    @org.springframework.web.bind.annotation.ResponseBody
    public com.eshoppingzone.website.dto.ProductAvailabilityDto checkAvailabilityAjax(@PathVariable("id") Long id) {
        try {
            return productClient.checkAvailability(id);
        } catch (Exception ex) {
            return com.eshoppingzone.website.dto.ProductAvailabilityDto.builder()
                    .productId(id)
                    .inStock(false)
                    .stockQuantity(0)
                    .message("Availability information currently unavailable")
                    .build();
        }
    }
}
