package com.eshoppingzone.website.controller;

import com.eshoppingzone.website.client.CartClient;
import com.eshoppingzone.website.client.OrderClient;
import com.eshoppingzone.website.client.ProfileClient;
import com.eshoppingzone.website.client.WalletClient;
import com.eshoppingzone.website.dto.*;
import com.eshoppingzone.website.session.SessionAuthHelper;
import com.eshoppingzone.website.session.UserSession;
import feign.FeignException;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/checkout")
public class CheckoutWebController {

    public static final String CHECKOUT_ADDRESS_SESSION_KEY = "CHECKOUT_SHIPPING_ADDRESS";

    private final CartClient cartClient;
    private final ProfileClient profileClient;
    private final WalletClient walletClient;
    private final OrderClient orderClient;
    private final SessionAuthHelper authHelper;

    public CheckoutWebController(
            CartClient cartClient,
            ProfileClient profileClient,
            WalletClient walletClient,
            OrderClient orderClient,
            SessionAuthHelper authHelper) {
        this.cartClient = cartClient;
        this.profileClient = profileClient;
        this.walletClient = walletClient;
        this.orderClient = orderClient;
        this.authHelper = authHelper;
    }

    private boolean isMerchantUser(HttpSession session) {
        return authHelper.isMerchant(session);
    }

    @GetMapping
    public String checkoutAddressPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/checkout";
        }
        if (isMerchantUser(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Merchants cannot perform customer checkout.");
            return "redirect:/merchant/dashboard";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        CartDto cart = null;
        try {
            cart = cartClient.getCartByCustomerId(currentUser.getProfileId());
        } catch (Exception ignored) {
        }

        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        model.addAttribute("cart", cart);
        model.addAttribute("cartCount", cart.getTotalItemCount());

        AddressDto address = (AddressDto) session.getAttribute(CHECKOUT_ADDRESS_SESSION_KEY);
        List<UserAddressDto> savedAddresses = new ArrayList<>();
        try {
            savedAddresses = profileClient.getAddresses(currentUser.getProfileId());
        } catch (Exception ignored) {
        }
        model.addAttribute("savedAddresses", savedAddresses);

        if (address == null) {
            // Check default saved address first
            UserAddressDto defaultAddr = savedAddresses.stream().filter(a -> Boolean.TRUE.equals(a.getIsDefault())).findFirst().orElse(null);
            if (defaultAddr != null) {
                address = new AddressDto(
                        defaultAddr.getFullName(),
                        defaultAddr.getMobileNumber(),
                        defaultAddr.getHouseNumber(),
                        defaultAddr.getStreetName(),
                        defaultAddr.getColonyName(),
                        defaultAddr.getCity(),
                        defaultAddr.getState(),
                        defaultAddr.getPincode()
                );
            } else {
                try {
                    UserProfileDto profile = profileClient.getProfileById(currentUser.getProfileId());
                    if (profile != null && profile.getAddress() != null) {
                        address = profile.getAddress();
                    }
                } catch (Exception ignored) {
                }
            }
        }
        if (address == null) {
            address = new AddressDto();
        }

        model.addAttribute("shippingAddress", address);
        return "checkout/checkout";
    }

    @PostMapping
    public String saveAddressAndProceed(
            @ModelAttribute AddressDto shippingAddress,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/checkout";
        }
        if (isMerchantUser(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Merchants cannot perform customer checkout.");
            return "redirect:/merchant/dashboard";
        }

        session.setAttribute(CHECKOUT_ADDRESS_SESSION_KEY, shippingAddress);
        return "redirect:/checkout/payment";
    }

    @GetMapping("/payment")
    public String paymentSelectionPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/checkout/payment";
        }
        if (isMerchantUser(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Merchants cannot perform customer checkout.");
            return "redirect:/merchant/dashboard";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        CartDto cart = null;
        try {
            cart = cartClient.getCartByCustomerId(currentUser.getProfileId());
        } catch (Exception ignored) {
        }

        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        AddressDto shippingAddress = (AddressDto) session.getAttribute(CHECKOUT_ADDRESS_SESSION_KEY);
        if (shippingAddress == null || shippingAddress.getStreetName() == null || shippingAddress.getStreetName().isBlank()) {
            return "redirect:/checkout";
        }

        double walletBalance = 0.0;
        try {
            WalletDto wallet = walletClient.getWallet(currentUser.getProfileId());
            if (wallet != null) {
                walletBalance = wallet.getCurrentBalance();
            }
        } catch (Exception ignored) {
        }

        boolean hasSufficientWalletBalance = walletBalance >= cart.getTotalPrice();

        model.addAttribute("cart", cart);
        model.addAttribute("cartCount", cart.getTotalItemCount());
        model.addAttribute("shippingAddress", shippingAddress);
        model.addAttribute("walletBalance", walletBalance);
        model.addAttribute("hasSufficientWalletBalance", hasSufficientWalletBalance);

        return "checkout/payment";
    }

    @PostMapping("/place-order")
    public String placeOrder(
            @RequestParam("paymentMethod") String paymentMethod,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/checkout";
        }
        if (isMerchantUser(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Merchants cannot place customer orders.");
            return "redirect:/merchant/dashboard";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);

        AddressDto shippingAddress = (AddressDto) session.getAttribute(CHECKOUT_ADDRESS_SESSION_KEY);
        if (shippingAddress == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Shipping address is missing. Please review your address.");
            return "redirect:/checkout";
        }

        if ("ONLINE".equalsIgnoreCase(paymentMethod)) {
            return "redirect:/checkout/online-payment";
        }

        try {
            CreateOrderRequest request = new CreateOrderRequest(
                    currentUser.getProfileId(),
                    paymentMethod,
                    shippingAddress
            );

            OrderDto order = orderClient.createOrder(request);
            session.removeAttribute(CHECKOUT_ADDRESS_SESSION_KEY);
            return "redirect:/orders/" + order.getOrderId() + "/confirmation";
        } catch (FeignException ex) {
            if (ex.status() == 400 || ex.getMessage().contains("Insufficient wallet balance")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Insufficient wallet balance. Please add funds to your wallet or choose Cash on Delivery.");
                return "redirect:/checkout/payment";
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Order failed: " + ex.getMessage());
            return "redirect:/checkout/payment";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to place order: " + ex.getMessage());
            return "redirect:/checkout/payment";
        }
    }

    @GetMapping("/online-payment")
    public String onlinePaymentGateway(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/checkout/payment";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        CartDto cart = null;
        try {
            cart = cartClient.getCartByCustomerId(currentUser.getProfileId());
        } catch (Exception ignored) {
        }

        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        AddressDto shippingAddress = (AddressDto) session.getAttribute(CHECKOUT_ADDRESS_SESSION_KEY);
        if (shippingAddress == null) {
            return "redirect:/checkout";
        }

        model.addAttribute("cart", cart);
        model.addAttribute("shippingAddress", shippingAddress);
        return "checkout/online-payment";
    }

    @PostMapping("/process-online-payment")
    public String processOnlinePayment(
            @RequestParam(value = "cardHolderName", required = false) String cardHolderName,
            @RequestParam(value = "paymentGatewayMode", defaultValue = "CARD") String paymentGatewayMode,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/checkout";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);

        AddressDto shippingAddress = (AddressDto) session.getAttribute(CHECKOUT_ADDRESS_SESSION_KEY);
        if (shippingAddress == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Shipping address is missing.");
            return "redirect:/checkout";
        }

        try {
            CreateOrderRequest request = new CreateOrderRequest(
                    currentUser.getProfileId(),
                    "ONLINE",
                    shippingAddress
            );

            OrderDto order = orderClient.createOrder(request);
            session.removeAttribute(CHECKOUT_ADDRESS_SESSION_KEY);
            redirectAttributes.addFlashAttribute("successMessage", "Online Payment of ₹" + String.format("%.2f", order.getOrderAmount()) + " via " + paymentGatewayMode + " authorized successfully!");
            return "redirect:/orders/" + order.getOrderId() + "/confirmation";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Online payment failed: " + ex.getMessage());
            return "redirect:/checkout/payment";
        }
    }
}
