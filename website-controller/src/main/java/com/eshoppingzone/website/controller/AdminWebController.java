package com.eshoppingzone.website.controller;

import com.eshoppingzone.website.client.OrderClient;
import com.eshoppingzone.website.client.ProductClient;
import com.eshoppingzone.website.client.ProfileClient;
import com.eshoppingzone.website.client.WalletClient;
import com.eshoppingzone.website.dto.*;
import com.eshoppingzone.website.session.SessionAuthHelper;
import com.eshoppingzone.website.session.UserSession;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminWebController {

    private static final Logger log = LoggerFactory.getLogger(AdminWebController.class);

    private final ProfileClient profileClient;
    private final OrderClient orderClient;
    private final ProductClient productClient;
    private final WalletClient walletClient;
    private final SessionAuthHelper authHelper;

    public AdminWebController(
            ProfileClient profileClient,
            OrderClient orderClient,
            ProductClient productClient,
            WalletClient walletClient,
            SessionAuthHelper authHelper) {
        this.profileClient = profileClient;
        this.orderClient = orderClient;
        this.productClient = productClient;
        this.walletClient = walletClient;
        this.authHelper = authHelper;
    }

    private boolean checkAdmin(HttpSession session) {
        return authHelper.isAdmin(session);
    }

    @GetMapping({"", "/", "/dashboard"})
    public String adminDashboard(HttpSession session, Model model) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/dashboard";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        List<UserProfileDto> customers = new ArrayList<>();
        List<UserProfileDto> merchants = new ArrayList<>();
        List<UserProfileDto> agents = new ArrayList<>();
        List<OrderDto> allOrders = new ArrayList<>();
        List<ProductDto> allProducts = new ArrayList<>();

        try {
            customers = profileClient.getProfilesByRole("CUSTOMER");
            merchants = profileClient.getProfilesByRole("MERCHANT");
            agents = profileClient.getProfilesByRole("DELIVERY_AGENT");
        } catch (Exception ex) {
            log.warn("Failed to fetch user profiles: {}", ex.getMessage());
        }

        try {
            allOrders = orderClient.getAllOrders();
        } catch (Exception ex) {
            log.warn("Failed to fetch orders: {}", ex.getMessage());
        }

        try {
            allProducts = productClient.getAllProductsForAdmin();
        } catch (Exception ex) {
            log.warn("Failed to fetch products: {}", ex.getMessage());
        }

        long activeAgents = agents.stream().filter(a -> Boolean.TRUE.equals(a.getActive())).count();
        long pendingOrders = allOrders.stream().filter(o -> "PLACED".equalsIgnoreCase(o.getOrderStatus()) || "CONFIRMED".equalsIgnoreCase(o.getOrderStatus())).count();
        long assignedOrders = allOrders.stream().filter(o -> "ASSIGNED".equalsIgnoreCase(o.getOrderStatus()) || "OUT_FOR_DELIVERY".equalsIgnoreCase(o.getOrderStatus())).count();
        long deliveredOrders = allOrders.stream().filter(o -> "DELIVERED".equalsIgnoreCase(o.getOrderStatus())).count();
        long cancelledOrders = allOrders.stream().filter(o -> "CANCELLED".equalsIgnoreCase(o.getOrderStatus())).count();

        double totalSales = allOrders.stream()
                .filter(o -> "DELIVERED".equalsIgnoreCase(o.getOrderStatus()) || "OUT_FOR_DELIVERY".equalsIgnoreCase(o.getOrderStatus()) || "CONFIRMED".equalsIgnoreCase(o.getOrderStatus()) || "ASSIGNED".equalsIgnoreCase(o.getOrderStatus()))
                .mapToDouble(o -> o.getOrderAmount() != null ? o.getOrderAmount() : 0.0)
                .sum();

        model.addAttribute("totalCustomers", customers.size());
        model.addAttribute("totalMerchants", merchants.size());
        model.addAttribute("totalAgents", agents.size());
        model.addAttribute("activeAgents", activeAgents);
        model.addAttribute("totalProducts", allProducts.size());
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("assignedOrders", assignedOrders);
        model.addAttribute("deliveredOrders", deliveredOrders);
        model.addAttribute("cancelledOrders", cancelledOrders);
        model.addAttribute("totalOrders", allOrders.size());
        model.addAttribute("totalSales", totalSales);
        model.addAttribute("recentOrders", allOrders.stream().limit(6).collect(Collectors.toList()));

        return "admin/dashboard";
    }

    // ==========================================
    // PHASE 1: CUSTOMER MANAGEMENT (111–115)
    // ==========================================
    @GetMapping("/customers")
    public String customersList(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/customers";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        List<UserProfileDto> customers = new ArrayList<>();
        try {
            if (query != null && !query.isBlank()) {
                customers = profileClient.searchCustomers(query);
            } else {
                customers = profileClient.getProfilesByRole("CUSTOMER");
            }
        } catch (Exception ex) {
            log.error("Failed to load customers: {}", ex.getMessage());
            model.addAttribute("errorMessage", "Unable to load customer records.");
        }

        PagedResult<UserProfileDto> pageData = PagedResult.from(customers, page, size);
        model.addAttribute("customers", pageData.getContent());
        model.addAttribute("pageData", pageData);
        model.addAttribute("baseUrl", "/admin/customers" + (query != null && !query.isBlank() ? "?query=" + query : ""));
        model.addAttribute("query", query);
        return "admin/customers";
    }

    @PostMapping("/customers/{id}/block")
    public String blockCustomer(
            @PathVariable("id") Long id,
            @RequestParam("blocked") boolean blocked,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/customers";
        }

        try {
            UserProfileDto updated = profileClient.blockUser(id, blocked);
            String msg = blocked ? "Customer '" + updated.getFullName() + "' has been BLOCKED." : "Customer '" + updated.getFullName() + "' has been UNBLOCKED.";
            redirectAttributes.addFlashAttribute("successMessage", msg);
        } catch (Exception ex) {
            log.error("Failed to update customer block status: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update status: " + ex.getMessage());
        }
        return "redirect:/admin/customers";
    }

    @PostMapping("/customers/{id}/toggle-status")
    public String toggleCustomerStatus(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/customers";
        }

        try {
            UserProfileDto updated = profileClient.toggleStatus(id);
            String status = Boolean.TRUE.equals(updated.getActive()) ? "Activated" : "Deactivated";
            redirectAttributes.addFlashAttribute("successMessage", "Customer '" + updated.getFullName() + "' status changed to " + status + ".");
        } catch (Exception ex) {
            log.error("Failed to toggle customer status: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating customer status: " + ex.getMessage());
        }
        return "redirect:/admin/customers";
    }

    // ==========================================
    // PHASE 2: MERCHANT MANAGEMENT (116–120)
    // ==========================================
    @GetMapping("/merchants")
    public String merchantsList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/merchants";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        List<UserProfileDto> merchants = new ArrayList<>();
        try {
            merchants = profileClient.getProfilesByRole("MERCHANT");
        } catch (Exception ex) {
            log.error("Failed to load merchants: {}", ex.getMessage());
            model.addAttribute("errorMessage", "Unable to load merchant records.");
        }

        PagedResult<UserProfileDto> pageData = PagedResult.from(merchants, page, size);
        model.addAttribute("merchants", pageData.getContent());
        model.addAttribute("pageData", pageData);
        model.addAttribute("baseUrl", "/admin/merchants");
        return "admin/merchants";
    }

    @PostMapping("/merchants/{id}/status")
    public String updateMerchantStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            @RequestParam(value = "reason", required = false) String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/merchants";
        }

        try {
            UserProfileDto updated = profileClient.updateAccountStatus(id, status, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Merchant '" + (updated.getBusinessName() != null ? updated.getBusinessName() : updated.getFullName()) + "' account status updated to " + status + ".");
        } catch (Exception ex) {
            log.error("Failed to update merchant status: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update merchant status: " + ex.getMessage());
        }
        return "redirect:/admin/merchants";
    }

    // ==========================================
    // PHASE 3: DELIVERY AGENTS & ZONES (121–125, 145)
    // ==========================================
    @GetMapping("/delivery-agents")
    public String deliveryAgentsList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/delivery-agents";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        List<UserProfileDto> allAgents = new ArrayList<>();
        try {
            allAgents = profileClient.getProfilesByRole("DELIVERY_AGENT");
        } catch (Exception ex) {
            log.warn("Failed to fetch delivery agents: {}", ex.getMessage());
            model.addAttribute("errorMessage", "Unable to load delivery agents.");
        }

        PagedResult<UserProfileDto> pageData = PagedResult.from(allAgents, page, size);
        model.addAttribute("agents", pageData.getContent());
        model.addAttribute("pageData", pageData);
        model.addAttribute("baseUrl", "/admin/delivery-agents");
        model.addAttribute("registerRequest", new RegisterRequest());
        return "admin/delivery-agents";
    }

    @PostMapping("/delivery-agents/add")
    public String addDeliveryAgent(
            @ModelAttribute RegisterRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/delivery-agents";
        }

        try {
            request.setRole("DELIVERY_AGENT");
            profileClient.createDeliveryAgent(request);
            redirectAttributes.addFlashAttribute("successMessage", "Delivery Agent '" + request.getFullName() + "' created successfully.");
        } catch (Exception ex) {
            log.error("Failed to create delivery agent: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create delivery agent: " + ex.getMessage());
        }
        return "redirect:/admin/delivery-agents";
    }

    @PostMapping("/delivery-agents/{id}/toggle-status")
    public String toggleAgentStatus(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/delivery-agents";
        }

        try {
            UserProfileDto updated = profileClient.toggleStatus(id);
            String status = Boolean.TRUE.equals(updated.getActive()) ? "Activated" : "Deactivated";
            redirectAttributes.addFlashAttribute("successMessage", "Agent '" + updated.getFullName() + "' status changed to " + status + ".");
        } catch (Exception ex) {
            log.error("Failed to toggle agent status: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating agent status: " + ex.getMessage());
        }
        return "redirect:/admin/delivery-agents";
    }

    @PostMapping("/delivery-agents/{id}/status")
    public String updateAgentStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            @RequestParam(value = "reason", required = false) String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/delivery-agents";
        }

        try {
            UserProfileDto updated = profileClient.updateAccountStatus(id, status, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Agent '" + updated.getFullName() + "' account status set to " + status + ".");
        } catch (Exception ex) {
            log.error("Failed to update agent status: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update agent status: " + ex.getMessage());
        }
        return "redirect:/admin/delivery-agents";
    }

    @PostMapping("/delivery-agents/{id}/zone")
    public String assignDeliveryAgentZone(
            @PathVariable("id") Long id,
            @RequestParam("zone") String zone,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/delivery-agents";
        }

        try {
            UserProfileDto updated = profileClient.assignDeliveryZone(id, zone);
            redirectAttributes.addFlashAttribute("successMessage", "Assigned zone '" + zone + "' to agent " + updated.getFullName() + ".");
        } catch (Exception ex) {
            log.error("Failed to assign zone: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to assign zone: " + ex.getMessage());
        }
        return "redirect:/admin/delivery-agents";
    }

    @GetMapping("/zones")
    public String deliveryZones(HttpSession session, Model model) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/zones";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        List<UserProfileDto> agents = new ArrayList<>();
        try {
            agents = profileClient.getProfilesByRole("DELIVERY_AGENT");
        } catch (Exception ex) {
            log.warn("Failed to load agents for zones: {}", ex.getMessage());
        }

        Map<String, List<UserProfileDto>> zoneMap = new LinkedHashMap<>();
        zoneMap.put("Central / Metro Tech Zone", new ArrayList<>());
        zoneMap.put("North Commercial Zone", new ArrayList<>());
        zoneMap.put("South Residential Zone", new ArrayList<>());
        zoneMap.put("East Industrial Hub", new ArrayList<>());
        zoneMap.put("West Express Corridor", new ArrayList<>());
        zoneMap.put("Unassigned", new ArrayList<>());

        for (UserProfileDto agent : agents) {
            String zone = agent.getDeliveryZone();
            if (zone != null && zoneMap.containsKey(zone)) {
                zoneMap.get(zone).add(agent);
            } else if (zone != null && !zone.isBlank()) {
                zoneMap.computeIfAbsent(zone, k -> new ArrayList<>()).add(agent);
            } else {
                zoneMap.get("Unassigned").add(agent);
            }
        }

        model.addAttribute("zoneMap", zoneMap);
        model.addAttribute("agents", agents);
        return "admin/zones";
    }

    // ==========================================
    // PHASE 4: PRODUCT MANAGEMENT (126–132)
    // ==========================================
    @GetMapping("/products")
    public String productsList(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/products";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        List<ProductDto> allProducts = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        try {
            allProducts = productClient.getAllProductsForAdmin();
            categories = productClient.getAllCategories();
        } catch (Exception ex) {
            log.error("Failed to load products for admin: {}", ex.getMessage());
            model.addAttribute("errorMessage", "Unable to load products.");
        }

        List<ProductDto> filtered = allProducts.stream()
                .filter(p -> {
                    if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
                        if (!category.equalsIgnoreCase(p.getCategory())) return false;
                    }
                    if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
                        if (!status.equalsIgnoreCase(p.getApprovalStatus())) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        PagedResult<ProductDto> pageData = PagedResult.from(filtered, page, size);
        model.addAttribute("products", pageData.getContent());
        model.addAttribute("pageData", pageData);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("baseUrl", "/admin/products");
        return "admin/products";
    }

    @PostMapping("/products/{id}/approval")
    public String updateProductApproval(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            @RequestParam(value = "reason", required = false) String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/products";
        }

        try {
            ProductDto updated = productClient.updateApprovalStatus(id, status, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Product '" + updated.getProductName() + "' status changed to " + status + ".");
        } catch (Exception ex) {
            log.error("Failed to update product approval: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update product status: " + ex.getMessage());
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/categories")
    public String manageCategories(HttpSession session, Model model) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/categories";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        List<String> categories = new ArrayList<>();
        List<String> subcategories = new ArrayList<>();
        try {
            categories = productClient.getAllCategories();
            subcategories = productClient.getAllProductTypes();
        } catch (Exception ex) {
            log.error("Failed to load categories: {}", ex.getMessage());
        }

        model.addAttribute("categories", categories);
        model.addAttribute("subcategories", subcategories);
        return "admin/categories";
    }

    @GetMapping("/brands")
    public String manageBrands(HttpSession session, Model model) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/brands";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        List<String> brands = new ArrayList<>();
        try {
            brands = productClient.getAllBrands();
        } catch (Exception ex) {
            log.error("Failed to load brands: {}", ex.getMessage());
        }

        model.addAttribute("brands", brands);
        return "admin/brands";
    }

    // ==========================================
    // PHASE 5: ORDER MANAGEMENT (133–139)
    // ==========================================
    @GetMapping("/orders")
    public String ordersList(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/orders";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        List<OrderDto> orders = new ArrayList<>();
        try {
            orders = orderClient.searchOrders(query, status);
        } catch (Exception ex) {
            log.error("Failed to search orders: {}", ex.getMessage());
            model.addAttribute("errorMessage", "Unable to load order records.");
        }

        PagedResult<OrderDto> pageData = PagedResult.from(orders, page, size);
        model.addAttribute("orders", pageData.getContent());
        model.addAttribute("pageData", pageData);
        model.addAttribute("query", query);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("baseUrl", "/admin/orders");
        return "admin/orders";
    }

    @PostMapping("/orders/{id}/cancel")
    public String adminCancelOrder(
            @PathVariable("id") Long id,
            @RequestParam(value = "reason", required = false) String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/orders";
        }

        try {
            OrderDto cancelled = orderClient.adminCancelOrder(id, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " has been CANCELLED and eligible refunds processed.");
        } catch (Exception ex) {
            log.error("Failed to cancel order: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to cancel order: " + ex.getMessage());
        }
        return "redirect:/admin/orders";
    }

    @GetMapping("/complaints")
    public String complaintsList(HttpSession session, Model model) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/complaints";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        List<CustomerComplaintDto> complaints = new ArrayList<>();
        try {
            complaints = orderClient.getAllComplaints();
        } catch (Exception ex) {
            log.error("Failed to load complaints: {}", ex.getMessage());
            model.addAttribute("errorMessage", "Unable to load customer complaints.");
        }

        model.addAttribute("complaints", complaints);
        return "admin/complaints";
    }

    @PostMapping("/complaints/{id}/status")
    public String updateComplaintStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            @RequestParam(value = "resolution", required = false) String resolution,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/complaints";
        }

        try {
            orderClient.updateComplaintStatus(id, status, resolution);
            redirectAttributes.addFlashAttribute("successMessage", "Complaint #" + id + " updated to " + status + ".");
        } catch (Exception ex) {
            log.error("Failed to update complaint: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update complaint: " + ex.getMessage());
        }
        return "redirect:/admin/complaints";
    }

    // ==========================================
    // PHASE 6: DELIVERY MANAGEMENT (140–144)
    // ==========================================
    @GetMapping("/deliveries")
    public String deliveriesList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/deliveries";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        List<OrderDto> allOrders = new ArrayList<>();
        try {
            allOrders = orderClient.getAllOrders();
        } catch (Exception ex) {
            log.error("Failed to load deliveries: {}", ex.getMessage());
        }

        List<OrderDto> deliveryRecords = allOrders.stream()
                .filter(o -> o.getDeliveryAgentId() != null || "ASSIGNED".equalsIgnoreCase(o.getOrderStatus()) || "OUT_FOR_DELIVERY".equalsIgnoreCase(o.getOrderStatus()) || "DELIVERED".equalsIgnoreCase(o.getOrderStatus()))
                .collect(Collectors.toList());

        PagedResult<OrderDto> pageData = PagedResult.from(deliveryRecords, page, size);
        model.addAttribute("deliveries", pageData.getContent());
        model.addAttribute("pageData", pageData);
        model.addAttribute("baseUrl", "/admin/deliveries");
        return "admin/deliveries";
    }

    @GetMapping("/assignments")
    public String orderAssignments(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/assignments";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        List<OrderDto> allOrders = new ArrayList<>();
        List<UserProfileDto> activeAgents = new ArrayList<>();
        try {
            allOrders = orderClient.getAllOrders();
            List<UserProfileDto> agents = profileClient.getProfilesByRole("DELIVERY_AGENT");
            activeAgents = agents.stream().filter(a -> Boolean.TRUE.equals(a.getActive())).collect(Collectors.toList());
        } catch (Exception ex) {
            log.warn("Failed to fetch assignment data: {}", ex.getMessage());
            model.addAttribute("errorMessage", "Unable to load assignments data.");
        }

        PagedResult<OrderDto> pageData = PagedResult.from(allOrders, page, size);
        model.addAttribute("orders", pageData.getContent());
        model.addAttribute("pageData", pageData);
        model.addAttribute("baseUrl", "/admin/assignments");
        model.addAttribute("activeAgents", activeAgents);
        return "admin/assignments";
    }

    @PostMapping("/assignments/assign")
    public String assignOrder(
            @RequestParam("orderId") Long orderId,
            @RequestParam("agentId") Long agentId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/assignments";
        }

        try {
            UserProfileDto agent = profileClient.getProfileById(agentId);
            AssignDeliveryAgentRequest req = new AssignDeliveryAgentRequest(agent.getProfileId(), agent.getFullName());
            orderClient.assignDeliveryAgent(orderId, req);
            redirectAttributes.addFlashAttribute("successMessage", "Order #" + orderId + " successfully assigned to " + agent.getFullName() + ".");
        } catch (Exception ex) {
            log.error("Failed to assign order: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to assign order: " + ex.getMessage());
        }
        return "redirect:/admin/assignments";
    }

    @GetMapping("/orders/{id}/track")
    public String trackOrderDetails(
            @PathVariable("id") Long id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/orders/" + id + "/track";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        try {
            OrderDto order = orderClient.getOrderById(id);
            model.addAttribute("order", order);
            return "orders/track";
        } catch (Exception ex) {
            log.error("Failed to load tracking details for order {}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to load tracking details for Order #" + id);
            return "redirect:/admin/assignments";
        }
    }

    // ==========================================
    // PHASE 7: PAYMENT MANAGEMENT (146–151)
    // ==========================================
    @GetMapping("/payments")
    public String paymentsList(
            @RequestParam(value = "filter", defaultValue = "ALL") String filter,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/payments";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        List<TransactionDto> walletTxs = new ArrayList<>();
        List<OrderDto> allOrders = new ArrayList<>();
        try {
            walletTxs = walletClient.getAllTransactions();
            allOrders = orderClient.getAllOrders();
        } catch (Exception ex) {
            log.error("Failed to load payments: {}", ex.getMessage());
        }

        List<OrderDto> codOrders = allOrders.stream()
                .filter(o -> "COD".equalsIgnoreCase(o.getPaymentMethod()))
                .collect(Collectors.toList());

        List<OrderDto> refundedOrders = allOrders.stream()
                .filter(o -> "REFUNDED".equalsIgnoreCase(o.getRefundStatus()))
                .collect(Collectors.toList());

        double totalWalletVolume = walletTxs.stream().mapToDouble(t -> t.getAmount() != null ? t.getAmount() : 0.0).sum();
        double totalCodVolume = codOrders.stream().mapToDouble(o -> o.getOrderAmount() != null ? o.getOrderAmount() : 0.0).sum();

        PagedResult<TransactionDto> pageData = PagedResult.from(walletTxs, page, size);
        model.addAttribute("transactions", pageData.getContent());
        model.addAttribute("pageData", pageData);
        model.addAttribute("codOrders", codOrders);
        model.addAttribute("refundedOrders", refundedOrders);
        model.addAttribute("totalWalletVolume", totalWalletVolume);
        model.addAttribute("totalCodVolume", totalCodVolume);
        model.addAttribute("currentFilter", filter);
        model.addAttribute("baseUrl", "/admin/payments");
        return "admin/payments";
    }

    // ==========================================
    // PHASE 8: REPORTS SUITE (152–158)
    // ==========================================
    @GetMapping("/reports")
    public String adminReports(
            @RequestParam(value = "tab", defaultValue = "sales") String tab,
            HttpSession session,
            Model model) {
        if (!checkAdmin(session)) {
            return "redirect:/login?redirect=/admin/reports";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        List<UserProfileDto> customers = new ArrayList<>();
        List<UserProfileDto> merchants = new ArrayList<>();
        List<UserProfileDto> agents = new ArrayList<>();
        List<OrderDto> orders = new ArrayList<>();
        List<ProductDto> products = new ArrayList<>();
        List<TransactionDto> transactions = new ArrayList<>();

        try {
            customers = profileClient.getProfilesByRole("CUSTOMER");
            merchants = profileClient.getProfilesByRole("MERCHANT");
            agents = profileClient.getProfilesByRole("DELIVERY_AGENT");
            orders = orderClient.getAllOrders();
            products = productClient.getAllProductsForAdmin();
            transactions = walletClient.getAllTransactions();
        } catch (Exception ex) {
            log.error("Failed to load reporting data: {}", ex.getMessage());
        }

        // 152. Customer Metrics
        long activeCust = customers.stream().filter(c -> Boolean.TRUE.equals(c.getActive()) && !Boolean.TRUE.equals(c.getBlocked())).count();
        long blockedCust = customers.stream().filter(c -> Boolean.TRUE.equals(c.getBlocked())).count();
        long inactiveCust = customers.stream().filter(c -> !Boolean.TRUE.equals(c.getActive())).count();

        // 153. Merchant Metrics
        long approvedMerchants = merchants.stream().filter(m -> "APPROVED".equalsIgnoreCase(m.getAccountStatus()) || (m.getAccountStatus() == null && Boolean.TRUE.equals(m.getActive()))).count();
        long pendingMerchants = merchants.stream().filter(m -> "PENDING_APPROVAL".equalsIgnoreCase(m.getAccountStatus())).count();
        long suspendedMerchants = merchants.stream().filter(m -> "SUSPENDED".equalsIgnoreCase(m.getAccountStatus()) || "REJECTED".equalsIgnoreCase(m.getAccountStatus())).count();

        // 154. Product Metrics
        long inStockProducts = products.stream().filter(p -> Boolean.TRUE.equals(p.getInStock()) && p.getStockQuantity() != null && p.getStockQuantity() > 10).count();
        long lowStockProducts = products.stream().filter(p -> p.getStockQuantity() != null && p.getStockQuantity() <= 10 && p.getStockQuantity() > 0).count();
        long outOfStockProducts = products.stream().filter(p -> !Boolean.TRUE.equals(p.getInStock()) || (p.getStockQuantity() != null && p.getStockQuantity() == 0)).count();

        // 155 & 156. Sales & Order Metrics
        double grossSales = orders.stream().mapToDouble(o -> o.getOrderAmount() != null ? o.getOrderAmount() : 0.0).sum();
        long deliveredCount = orders.stream().filter(o -> "DELIVERED".equalsIgnoreCase(o.getOrderStatus())).count();
        long cancelledCount = orders.stream().filter(o -> "CANCELLED".equalsIgnoreCase(o.getOrderStatus())).count();
        long inTransitCount = orders.stream().filter(o -> "OUT_FOR_DELIVERY".equalsIgnoreCase(o.getOrderStatus()) || "ASSIGNED".equalsIgnoreCase(o.getOrderStatus())).count();

        // 157. Delivery Metrics
        long totalDispatches = orders.stream().filter(o -> o.getDeliveryAgentId() != null).count();
        long failedDeliveries = orders.stream().filter(o -> o.getDeliveryFailureReason() != null).count();

        // 158. Payment Metrics
        double walletVolume = transactions.stream().mapToDouble(t -> t.getAmount() != null ? t.getAmount() : 0.0).sum();
        long codCount = orders.stream().filter(o -> "COD".equalsIgnoreCase(o.getPaymentMethod())).count();
        long onlineCount = orders.stream().filter(o -> !"COD".equalsIgnoreCase(o.getPaymentMethod())).count();

        model.addAttribute("activeTab", tab);
        model.addAttribute("customers", customers);
        model.addAttribute("merchants", merchants);
        model.addAttribute("agents", agents);
        model.addAttribute("orders", orders);
        model.addAttribute("products", products);
        model.addAttribute("transactions", transactions);

        model.addAttribute("activeCust", activeCust);
        model.addAttribute("blockedCust", blockedCust);
        model.addAttribute("inactiveCust", inactiveCust);

        model.addAttribute("approvedMerchants", approvedMerchants);
        model.addAttribute("pendingMerchants", pendingMerchants);
        model.addAttribute("suspendedMerchants", suspendedMerchants);

        model.addAttribute("inStockProducts", inStockProducts);
        model.addAttribute("lowStockProducts", lowStockProducts);
        model.addAttribute("outOfStockProducts", outOfStockProducts);

        model.addAttribute("grossSales", grossSales);
        model.addAttribute("deliveredCount", deliveredCount);
        model.addAttribute("cancelledCount", cancelledCount);
        model.addAttribute("inTransitCount", inTransitCount);

        model.addAttribute("totalDispatches", totalDispatches);
        model.addAttribute("failedDeliveries", failedDeliveries);

        model.addAttribute("walletVolume", walletVolume);
        model.addAttribute("codCount", codCount);
        model.addAttribute("onlineCount", onlineCount);

        return "admin/reports";
    }
}
