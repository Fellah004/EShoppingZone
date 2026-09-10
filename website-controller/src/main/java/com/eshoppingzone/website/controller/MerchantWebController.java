package com.eshoppingzone.website.controller;

import com.eshoppingzone.website.client.OrderClient;
import com.eshoppingzone.website.client.ProductClient;
import com.eshoppingzone.website.client.ProfileClient;
import com.eshoppingzone.website.dto.*;
import com.eshoppingzone.website.session.SessionAuthHelper;
import com.eshoppingzone.website.session.UserSession;
import feign.FeignException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/merchant")
public class MerchantWebController {

    private final ProductClient productClient;
    private final OrderClient orderClient;
    private final ProfileClient profileClient;
    private final SessionAuthHelper authHelper;

    public MerchantWebController(
            ProductClient productClient,
            OrderClient orderClient,
            ProfileClient profileClient,
            SessionAuthHelper authHelper) {
        this.productClient = productClient;
        this.orderClient = orderClient;
        this.profileClient = profileClient;
        this.authHelper = authHelper;
    }

    private boolean checkMerchantAccess(HttpSession session, RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return false;
        }
        UserSession user = authHelper.getCurrentUser(session);
        if (!user.isMerchant()) {
            if (redirectAttributes != null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Access Denied: Merchant account required to access merchant operations.");
            }
            return false;
        }
        return true;
    }

    // ==========================================
    // PART 6 — DASHBOARD (Features 70, 71, 72, 73, 74, 75)
    // ==========================================
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/merchant/dashboard";
        }
        if (!checkMerchantAccess(session, redirectAttributes)) {
            return "redirect:/";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        List<ProductDto> products = new ArrayList<>();
        List<MerchantSaleDto> sales = new ArrayList<>();
        List<OrderDto> merchantOrders = new ArrayList<>();
        List<ProductDto> lowStockProducts = new ArrayList<>();
        List<ReturnRequestDto> returnRequests = new ArrayList<>();
        UserProfileDto profile = null;

        try {
            products = productClient.getProductsByMerchant(currentUser.getProfileId());
            sales = orderClient.getMerchantSales(currentUser.getProfileId());
            merchantOrders = orderClient.getMerchantOrders(currentUser.getProfileId(), null);
            lowStockProducts = productClient.getLowStockProducts(currentUser.getProfileId());
            returnRequests = orderClient.getMerchantReturnRequests(currentUser.getProfileId());
            profile = profileClient.getProfileById(currentUser.getProfileId());
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Unable to load complete merchant workspace data: " + ex.getMessage());
        }

        // Calculate Metrics
        double totalRevenue = sales.stream()
                .mapToDouble(s -> s.getTotalAmount() != null ? s.getTotalAmount() : 0.0)
                .sum();

        long totalOrdersCount = merchantOrders.size();
        long pendingOrdersCount = merchantOrders.stream()
                .filter(o -> o.getOrderStatus() != null && "PLACED".equalsIgnoreCase(o.getOrderStatus()))
                .count();
        long activeProductsCount = products.stream()
                .filter(p -> p.getActive() == null || Boolean.TRUE.equals(p.getActive()))
                .count();
        long pendingReturnsCount = returnRequests.stream()
                .filter(r -> "REQUESTED".equalsIgnoreCase(r.getStatus()))
                .count();

        // Best selling products (Feature 72)
        Map<String, Integer> productSalesMap = new HashMap<>();
        for (MerchantSaleDto sale : sales) {
            String pName = sale.getProductName() != null ? sale.getProductName() : "Product #" + sale.getProductId();
            int qty = sale.getQuantity() != null ? sale.getQuantity() : 1;
            productSalesMap.put(pName, productSalesMap.getOrDefault(pName, 0) + qty);
        }
        List<Map.Entry<String, Integer>> topProducts = productSalesMap.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .collect(Collectors.toList());

        // Recent sales/orders (Feature 73)
        List<MerchantSaleDto> recentSales = sales.stream()
                .limit(10)
                .collect(Collectors.toList());

        model.addAttribute("products", products);
        model.addAttribute("sales", sales);
        model.addAttribute("recentSales", recentSales);
        model.addAttribute("topProducts", topProducts);
        model.addAttribute("lowStockProducts", lowStockProducts);
        model.addAttribute("returnRequests", returnRequests);
        model.addAttribute("merchantOrders", merchantOrders);
        model.addAttribute("profile", profile);

        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalOrdersCount", totalOrdersCount);
        model.addAttribute("pendingOrdersCount", pendingOrdersCount);
        model.addAttribute("totalProducts", products.size());
        model.addAttribute("activeProductsCount", activeProductsCount);
        model.addAttribute("lowStockCount", lowStockProducts.size());
        model.addAttribute("pendingReturnsCount", pendingReturnsCount);

        return "merchant/dashboard";
    }

    // ==========================================
    // PART 2 — MERCHANT BUSINESS PROFILE (Features 44, 45, 46)
    // ==========================================
    @GetMapping("/profile")
    public String profilePage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/merchant/profile";
        }
        if (!checkMerchantAccess(session, redirectAttributes)) {
            return "redirect:/";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        try {
            UserProfileDto profile = profileClient.getProfileById(currentUser.getProfileId());
            model.addAttribute("profile", profile);
            UpdateProfileRequest updateRequest = new UpdateProfileRequest(
                    profile.getFullName(),
                    profile.getMobileNumber(),
                    profile.getDateOfBirth(),
                    profile.getGender(),
                    profile.getProfileImage(),
                    profile.getAddress() != null ? profile.getAddress() : new AddressDto()
            );
            updateRequest.setAbout(profile.getAbout());
            updateRequest.setBusinessName(profile.getBusinessName());
            model.addAttribute("updateRequest", updateRequest);
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Failed to load merchant profile: " + ex.getMessage());
        }

        return "merchant/profile";
    }

    @PostMapping("/profile")
    public String handleUpdateProfile(
            @Valid @ModelAttribute("updateRequest") UpdateProfileRequest request,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login";
        }
        if (!checkMerchantAccess(session, redirectAttributes)) {
            return "redirect:/";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);

        if (bindingResult.hasErrors()) {
            model.addAttribute("currentUser", currentUser);
            return "merchant/profile";
        }

        try {
            UserProfileDto updated = profileClient.updateProfile(currentUser.getProfileId(), request);
            currentUser.setFullName(updated.getFullName());
            redirectAttributes.addFlashAttribute("successMessage", "Merchant business profile and store details updated successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update profile: " + ex.getMessage());
        }

        return "redirect:/merchant/profile";
    }

    // ==========================================
    // PART 3 — PRODUCT MANAGEMENT (Features 47, 48, 49, 50, 51, 52, 53, 54, 55)
    // ==========================================
    @GetMapping("/products")
    public String myProducts(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String statusFilter,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/merchant/products";
        }
        if (!checkMerchantAccess(session, redirectAttributes)) {
            return "redirect:/";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        List<ProductDto> allProducts = new ArrayList<>();
        try {
            allProducts = productClient.getProductsByMerchant(currentUser.getProfileId());
            if ("active".equalsIgnoreCase(statusFilter)) {
                allProducts = allProducts.stream().filter(p -> p.getActive() == null || Boolean.TRUE.equals(p.getActive())).collect(Collectors.toList());
            } else if ("inactive".equalsIgnoreCase(statusFilter)) {
                allProducts = allProducts.stream().filter(p -> Boolean.FALSE.equals(p.getActive())).collect(Collectors.toList());
            }
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Unable to load products: " + ex.getMessage());
        }

        com.eshoppingzone.website.dto.PagedResult<ProductDto> pageData = com.eshoppingzone.website.dto.PagedResult.from(allProducts, page, size);
        model.addAttribute("products", pageData.getContent());
        model.addAttribute("pageData", pageData);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("baseUrl", "/merchant/products");
        return "merchant/products";
    }

    @GetMapping("/products/add")
    public String addProductPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/merchant/products/add";
        }
        if (!checkMerchantAccess(session, redirectAttributes)) {
            return "redirect:/";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        if (!model.containsAttribute("productRequest")) {
            ProductCreateRequest request = new ProductCreateRequest();
            request.setMerchantId(currentUser.getProfileId());
            request.setStockQuantity(100);
            request.setActive(true);
            request.setDiscountPercent(0.0);
            request.setLowStockThreshold(10);
            model.addAttribute("productRequest", request);
        }

        try {
            model.addAttribute("categories", productClient.getAllCategories());
            model.addAttribute("types", productClient.getAllProductTypes());
        } catch (Exception ignored) {
        }

        return "merchant/add-product";
    }

    @PostMapping("/products/add")
    public String handleAddProduct(
            @Valid @ModelAttribute("productRequest") ProductCreateRequest request,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login";
        }
        if (!checkMerchantAccess(session, redirectAttributes)) {
            return "redirect:/";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);

        if (bindingResult.hasErrors()) {
            model.addAttribute("currentUser", currentUser);
            try {
                model.addAttribute("categories", productClient.getAllCategories());
                model.addAttribute("types", productClient.getAllProductTypes());
            } catch (Exception ignored) {
            }
            return "merchant/add-product";
        }

        try {
            request.setMerchantId(currentUser.getProfileId());
            ProductDto created = productClient.addProduct(request);
            redirectAttributes.addFlashAttribute("successMessage", "Product '" + created.getProductName() + "' added to catalog successfully!");
            return "redirect:/merchant/products";
        } catch (FeignException ex) {
            if (ex.status() == 403) {
                redirectAttributes.addFlashAttribute("errorMessage", "Access Denied: You do not have merchant permissions to add products.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to add product: " + ex.getMessage());
            }
            return "redirect:/merchant/products/add";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to add product: " + ex.getMessage());
            return "redirect:/merchant/products/add";
        }
    }

    @GetMapping("/products/edit/{id}")
    public String editProductPage(
            @PathVariable("id") Long id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/merchant/products/edit/" + id;
        }
        if (!checkMerchantAccess(session, redirectAttributes)) {
            return "redirect:/";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        try {
            ProductDto product = productClient.getProductById(id);
            ProductUpdateRequest updateRequest = new ProductUpdateRequest(
                    product.getProductName(),
                    product.getProductType(),
                    product.getCategory(),
                    product.getManufacturer(),
                    product.getImage(),
                    product.getPrice(),
                    product.getDescription(),
                    product.getSpecification()
            );
            updateRequest.setMerchantId(currentUser.getProfileId());
            updateRequest.setStockQuantity(product.getStockQuantity());
            updateRequest.setInStock(product.getInStock());
            updateRequest.setActive(product.getActive());
            updateRequest.setDiscountPercent(product.getDiscountPercent());
            updateRequest.setLowStockThreshold(product.getLowStockThreshold());

            model.addAttribute("product", product);
            model.addAttribute("updateRequest", updateRequest);
            model.addAttribute("categories", productClient.getAllCategories());
            model.addAttribute("types", productClient.getAllProductTypes());
            model.addAttribute("variants", product.getVariants() != null ? product.getVariants() : new ArrayList<>());
            model.addAttribute("newVariant", new VariantCreateRequest());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Product not found: " + ex.getMessage());
            return "redirect:/merchant/products";
        }

        return "merchant/edit-product";
    }

    @PostMapping("/products/edit/{id}")
    public String handleEditProduct(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("updateRequest") ProductUpdateRequest request,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login";
        }
        if (!checkMerchantAccess(session, redirectAttributes)) {
            return "redirect:/";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);

        if (bindingResult.hasErrors()) {
            model.addAttribute("currentUser", currentUser);
            try {
                model.addAttribute("product", productClient.getProductById(id));
                model.addAttribute("categories", productClient.getAllCategories());
                model.addAttribute("types", productClient.getAllProductTypes());
            } catch (Exception ignored) {
            }
            return "merchant/edit-product";
        }

        try {
            request.setMerchantId(currentUser.getProfileId());
            ProductDto updated = productClient.updateProduct(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Product '" + updated.getProductName() + "' updated successfully!");
            return "redirect:/merchant/products";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update product: " + ex.getMessage());
            return "redirect:/merchant/products/edit/" + id;
        }
    }

    @PostMapping("/products/delete/{id}")
    public String handleDeleteProduct(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login";
        }
        if (!checkMerchantAccess(session, redirectAttributes)) {
            return "redirect:/";
        }

        try {
            productClient.deleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product deleted successfully from catalog.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete product: " + ex.getMessage());
        }

        return "redirect:/merchant/products";
    }

    // Feature 50: Activate / Deactivate Product
    @PostMapping("/products/{id}/toggle-active")
    public String handleToggleActive(
            @PathVariable("id") Long id,
            @RequestParam(value = "redirect", defaultValue = "/merchant/products") String redirect,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) return "redirect:/login";
        if (!checkMerchantAccess(session, redirectAttributes)) return "redirect:/";

        try {
            UserSession currentUser = authHelper.getCurrentUser(session);
            ProductDto updated = productClient.toggleProductActive(id, currentUser.getProfileId());
            boolean active = Boolean.TRUE.equals(updated.getActive());
            redirectAttributes.addFlashAttribute("successMessage", "Product '" + updated.getProductName() + "' is now " + (active ? "ACTIVE (Live in catalog)" : "DEACTIVATED (Hidden from customers)") + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to change product status: " + ex.getMessage());
        }

        return "redirect:" + redirect;
    }

    // Feature 51: Upload Product Image URL
    @PostMapping("/products/{id}/image")
    public String handleUploadImage(
            @PathVariable("id") Long id,
            @RequestParam("imageUrl") String imageUrl,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) return "redirect:/login";
        if (!checkMerchantAccess(session, redirectAttributes)) return "redirect:/";

        try {
            UserSession currentUser = authHelper.getCurrentUser(session);
            productClient.uploadImage(id, imageUrl, currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "Product image updated successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update product image: " + ex.getMessage());
        }

        return "redirect:/merchant/products/edit/" + id;
    }

    // Feature 54: Set Product Discount
    @PostMapping("/products/{id}/discount")
    public String handleSetDiscount(
            @PathVariable("id") Long id,
            @RequestParam("discountPercent") Double discountPercent,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) return "redirect:/login";
        if (!checkMerchantAccess(session, redirectAttributes)) return "redirect:/";

        try {
            UserSession currentUser = authHelper.getCurrentUser(session);
            ProductDto updated = productClient.setDiscount(id, discountPercent, currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "Discount of " + updated.getDiscountPercent() + "% applied. New effective price: ₹" + String.format("%.2f", updated.getDiscountedPrice()) + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to apply discount: " + ex.getMessage());
        }

        return "redirect:/merchant/products/edit/" + id;
    }

    // Feature 55: Product Variants (Add & Delete)
    @PostMapping("/products/{id}/variants")
    public String handleAddVariant(
            @PathVariable("id") Long id,
            @ModelAttribute VariantCreateRequest variantRequest,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) return "redirect:/login";
        if (!checkMerchantAccess(session, redirectAttributes)) return "redirect:/";

        try {
            UserSession currentUser = authHelper.getCurrentUser(session);
            productClient.addVariant(id, variantRequest, currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "Product variant (" + variantRequest.getVariantName() + ": " + variantRequest.getVariantValue() + ") added successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to add variant: " + ex.getMessage());
        }

        return "redirect:/merchant/products/edit/" + id;
    }

    @PostMapping("/products/{id}/variants/delete/{variantId}")
    public String handleDeleteVariant(
            @PathVariable("id") Long id,
            @PathVariable("variantId") Long variantId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) return "redirect:/login";
        if (!checkMerchantAccess(session, redirectAttributes)) return "redirect:/";

        try {
            UserSession currentUser = authHelper.getCurrentUser(session);
            productClient.deleteVariant(id, variantId, currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "Variant removed successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete variant: " + ex.getMessage());
        }

        return "redirect:/merchant/products/edit/" + id;
    }

    // ==========================================
    // PART 4 — INVENTORY MANAGEMENT (Features 56, 57, 58, 59, 60, 61)
    // ==========================================
    @GetMapping("/inventory")
    public String inventoryOverview(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "filter", required = false) String filter,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/merchant/inventory";
        }
        if (!checkMerchantAccess(session, redirectAttributes)) {
            return "redirect:/";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        List<ProductDto> products = new ArrayList<>();
        List<ProductDto> lowStockProducts = new ArrayList<>();
        try {
            products = productClient.getProductsByMerchant(currentUser.getProfileId());
            lowStockProducts = productClient.getLowStockProducts(currentUser.getProfileId());

            if ("low_stock".equalsIgnoreCase(filter)) {
                products = lowStockProducts;
            } else if ("out_of_stock".equalsIgnoreCase(filter)) {
                products = products.stream().filter(p -> p.getStockQuantity() == null || p.getStockQuantity() <= 0 || Boolean.FALSE.equals(p.getInStock())).collect(Collectors.toList());
            }
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Failed to load inventory details: " + ex.getMessage());
        }

        com.eshoppingzone.website.dto.PagedResult<ProductDto> pageData = com.eshoppingzone.website.dto.PagedResult.from(products, page, size);
        model.addAttribute("products", pageData.getContent());
        model.addAttribute("pageData", pageData);
        model.addAttribute("filter", filter);
        model.addAttribute("lowStockCount", lowStockProducts.size());
        model.addAttribute("baseUrl", "/merchant/inventory");
        return "merchant/inventory";
    }

    // Feature 56: Add stock quantity
    @PostMapping("/inventory/{id}/add-stock")
    public String handleAddStock(
            @PathVariable("id") Long id,
            @RequestParam("quantity") Integer quantity,
            @RequestParam(value = "redirect", defaultValue = "/merchant/inventory") String redirect,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) return "redirect:/login";
        if (!checkMerchantAccess(session, redirectAttributes)) return "redirect:/";

        try {
            UserSession currentUser = authHelper.getCurrentUser(session);
            ProductDto updated = productClient.addStock(id, quantity, currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "Added " + quantity + " units to '" + updated.getProductName() + "'. New stock: " + updated.getStockQuantity() + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to add stock: " + ex.getMessage());
        }

        return "redirect:" + redirect;
    }

    // Feature 57: Update stock quantity
    @PostMapping("/inventory/{id}/update-stock")
    public String handleUpdateStock(
            @PathVariable("id") Long id,
            @RequestParam("quantity") Integer quantity,
            @RequestParam(value = "redirect", defaultValue = "/merchant/inventory") String redirect,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) return "redirect:/login";
        if (!checkMerchantAccess(session, redirectAttributes)) return "redirect:/";

        try {
            UserSession currentUser = authHelper.getCurrentUser(session);
            StockUpdateRequest req = StockUpdateRequest.builder().quantity(quantity).operation("SET").build();
            ProductDto updated = productClient.updateStock(id, req, currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "Stock updated for '" + updated.getProductName() + "' to " + updated.getStockQuantity() + " units.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update stock: " + ex.getMessage());
        }

        return "redirect:" + redirect;
    }

    // Feature 60: Mark product as out of stock
    @PostMapping("/inventory/{id}/out-of-stock")
    public String handleMarkOutOfStock(
            @PathVariable("id") Long id,
            @RequestParam(value = "redirect", defaultValue = "/merchant/inventory") String redirect,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) return "redirect:/login";
        if (!checkMerchantAccess(session, redirectAttributes)) return "redirect:/";

        try {
            UserSession currentUser = authHelper.getCurrentUser(session);
            ProductDto updated = productClient.markOutOfStock(id, currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "Product '" + updated.getProductName() + "' marked as OUT OF STOCK.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to mark out of stock: " + ex.getMessage());
        }

        return "redirect:" + redirect;
    }

    // Feature 61: Restock product
    @PostMapping("/inventory/{id}/restock")
    public String handleRestock(
            @PathVariable("id") Long id,
            @RequestParam(value = "quantity", defaultValue = "50") Integer quantity,
            @RequestParam(value = "redirect", defaultValue = "/merchant/inventory") String redirect,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) return "redirect:/login";
        if (!checkMerchantAccess(session, redirectAttributes)) return "redirect:/";

        try {
            UserSession currentUser = authHelper.getCurrentUser(session);
            ProductDto updated = productClient.restock(id, quantity, currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "Restocked '" + updated.getProductName() + "' with +" + quantity + " units. Total stock: " + updated.getStockQuantity() + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to restock product: " + ex.getMessage());
        }

        return "redirect:" + redirect;
    }

    // ==========================================
    // PART 5 — MERCHANT ORDER MANAGEMENT (Features 62, 63, 64, 65, 66, 67, 68, 69)
    // ==========================================
    @GetMapping("/orders")
    public String merchantOrders(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String statusFilter,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/merchant/orders";
        }
        if (!checkMerchantAccess(session, redirectAttributes)) {
            return "redirect:/";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        List<OrderDto> orders = new ArrayList<>();
        try {
            orders = orderClient.getMerchantOrders(currentUser.getProfileId(), statusFilter);
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Failed to load merchant orders: " + ex.getMessage());
        }

        com.eshoppingzone.website.dto.PagedResult<OrderDto> pageData = com.eshoppingzone.website.dto.PagedResult.from(orders, page, size);
        model.addAttribute("orders", pageData.getContent());
        model.addAttribute("pageData", pageData);
        model.addAttribute("statusFilter", statusFilter != null ? statusFilter.toUpperCase() : "ALL");
        model.addAttribute("baseUrl", "/merchant/orders");
        return "merchant/orders";
    }

    // Feature 63: Accept order
    @PostMapping("/orders/{id}/accept")
    public String handleAcceptOrder(
            @PathVariable("id") Long id,
            @RequestParam(value = "redirect", defaultValue = "/merchant/orders") String redirect,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) return "redirect:/login";
        if (!checkMerchantAccess(session, redirectAttributes)) return "redirect:/";

        try {
            UserSession currentUser = authHelper.getCurrentUser(session);
            orderClient.acceptOrder(id, currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " has been ACCEPTED and confirmed!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to accept order #" + id + ": " + ex.getMessage());
        }

        return "redirect:" + redirect;
    }

    // Feature 64: Reject order
    @PostMapping("/orders/{id}/reject")
    public String handleRejectOrder(
            @PathVariable("id") Long id,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "redirect", defaultValue = "/merchant/orders") String redirect,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) return "redirect:/login";
        if (!checkMerchantAccess(session, redirectAttributes)) return "redirect:/";

        try {
            UserSession currentUser = authHelper.getCurrentUser(session);
            orderClient.rejectOrder(id, currentUser.getProfileId(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " was REJECTED and customer notified/refunded.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reject order #" + id + ": " + ex.getMessage());
        }

        return "redirect:" + redirect;
    }

    // Feature 65: Process order
    @PostMapping("/orders/{id}/process")
    public String handleProcessOrder(
            @PathVariable("id") Long id,
            @RequestParam(value = "redirect", defaultValue = "/merchant/orders") String redirect,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) return "redirect:/login";
        if (!checkMerchantAccess(session, redirectAttributes)) return "redirect:/";

        try {
            UserSession currentUser = authHelper.getCurrentUser(session);
            orderClient.processOrder(id, currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " marked as IN PROCESS.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to process order #" + id + ": " + ex.getMessage());
        }

        return "redirect:" + redirect;
    }

    // Feature 66: Pack product
    @PostMapping("/orders/{id}/pack")
    public String handlePackOrder(
            @PathVariable("id") Long id,
            @RequestParam(value = "redirect", defaultValue = "/merchant/orders") String redirect,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) return "redirect:/login";
        if (!checkMerchantAccess(session, redirectAttributes)) return "redirect:/";

        try {
            UserSession currentUser = authHelper.getCurrentUser(session);
            orderClient.packOrder(id, currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " marked as PACKED.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to pack order #" + id + ": " + ex.getMessage());
        }

        return "redirect:" + redirect;
    }

    // Feature 67: Mark product ready for pickup
    @PostMapping("/orders/{id}/ready-pickup")
    public String handleReadyForPickup(
            @PathVariable("id") Long id,
            @RequestParam(value = "redirect", defaultValue = "/merchant/orders") String redirect,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) return "redirect:/login";
        if (!checkMerchantAccess(session, redirectAttributes)) return "redirect:/";

        try {
            UserSession currentUser = authHelper.getCurrentUser(session);
            orderClient.readyForPickup(id, currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " marked READY FOR PICKUP. Delivery agent & admins notified!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to mark order #" + id + " ready for pickup: " + ex.getMessage());
        }

        return "redirect:" + redirect;
    }

    // ==========================================
    // PART 7 — RETURNS MANAGEMENT (Features 76, 77, 78, 79)
    // ==========================================
    @GetMapping("/returns")
    public String merchantReturns(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String statusFilter,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/merchant/returns";
        }
        if (!checkMerchantAccess(session, redirectAttributes)) {
            return "redirect:/";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        List<ReturnRequestDto> returns = new ArrayList<>();
        try {
            returns = orderClient.getMerchantReturnRequests(currentUser.getProfileId());
            if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
                returns = returns.stream()
                        .filter(r -> r.getStatus() != null && r.getStatus().equalsIgnoreCase(statusFilter))
                        .collect(Collectors.toList());
            }
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Failed to load return requests: " + ex.getMessage());
        }

        com.eshoppingzone.website.dto.PagedResult<ReturnRequestDto> pageData = com.eshoppingzone.website.dto.PagedResult.from(returns, page, size);
        model.addAttribute("returns", pageData.getContent());
        model.addAttribute("pageData", pageData);
        model.addAttribute("statusFilter", statusFilter != null ? statusFilter.toUpperCase() : "ALL");
        model.addAttribute("baseUrl", "/merchant/returns");
        return "merchant/returns";
    }

    // Feature 77: Approve Return
    @PostMapping("/returns/{returnId}/approve")
    public String handleApproveReturn(
            @PathVariable("returnId") Long returnId,
            @RequestParam(value = "redirect", defaultValue = "/merchant/returns") String redirect,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) return "redirect:/login";
        if (!checkMerchantAccess(session, redirectAttributes)) return "redirect:/";

        try {
            UserSession currentUser = authHelper.getCurrentUser(session);
            orderClient.approveReturn(returnId, currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "Return request #" + returnId + " APPROVED. Customer requested to ship item.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to approve return #" + returnId + ": " + ex.getMessage());
        }

        return "redirect:" + redirect;
    }

    // Feature 77: Reject Return
    @PostMapping("/returns/{returnId}/reject")
    public String handleRejectReturn(
            @PathVariable("returnId") Long returnId,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "redirect", defaultValue = "/merchant/returns") String redirect,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) return "redirect:/login";
        if (!checkMerchantAccess(session, redirectAttributes)) return "redirect:/";

        try {
            UserSession currentUser = authHelper.getCurrentUser(session);
            orderClient.rejectReturn(returnId, currentUser.getProfileId(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Return request #" + returnId + " REJECTED.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reject return #" + returnId + ": " + ex.getMessage());
        }

        return "redirect:" + redirect;
    }

    // Feature 78: Receive returned product
    @PostMapping("/returns/{returnId}/receive")
    public String handleReceiveReturnedProduct(
            @PathVariable("returnId") Long returnId,
            @RequestParam(value = "redirect", defaultValue = "/merchant/returns") String redirect,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) return "redirect:/login";
        if (!checkMerchantAccess(session, redirectAttributes)) return "redirect:/";

        try {
            UserSession currentUser = authHelper.getCurrentUser(session);
            orderClient.receiveReturnedProduct(returnId, currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "Returned product marked as RECEIVED for return #" + returnId + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to receive returned product #" + returnId + ": " + ex.getMessage());
        }

        return "redirect:" + redirect;
    }

    // Feature 79: Update stock after return & complete refund
    @PostMapping("/returns/{returnId}/restock")
    public String handleRestockAndCompleteReturn(
            @PathVariable("returnId") Long returnId,
            @RequestParam(value = "redirect", defaultValue = "/merchant/returns") String redirect,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) return "redirect:/login";
        if (!checkMerchantAccess(session, redirectAttributes)) return "redirect:/";

        try {
            UserSession currentUser = authHelper.getCurrentUser(session);
            orderClient.restockAndCompleteReturn(returnId, currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "Return #" + returnId + " RESTOCKED into catalog and COMPLETED with wallet refund!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to restock return #" + returnId + ": " + ex.getMessage());
        }

        return "redirect:" + redirect;
    }

    // Existing /merchant/sales page maintained for full backward compatibility
    @GetMapping("/sales")
    public String mySales(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/merchant/sales";
        }
        if (!checkMerchantAccess(session, redirectAttributes)) {
            return "redirect:/";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        List<MerchantSaleDto> allSales = new ArrayList<>();
        try {
            allSales = orderClient.getMerchantSales(currentUser.getProfileId());
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Unable to load sales history: " + ex.getMessage());
        }

        com.eshoppingzone.website.dto.PagedResult<MerchantSaleDto> pageData = com.eshoppingzone.website.dto.PagedResult.from(allSales, page, size);
        model.addAttribute("sales", pageData.getContent());
        model.addAttribute("pageData", pageData);
        model.addAttribute("baseUrl", "/merchant/sales");
        return "merchant/sales";
    }
}
