package com.eshoppingzone.website.controller;

import com.eshoppingzone.website.client.CartClient;
import com.eshoppingzone.website.dto.AddToCartRequest;
import com.eshoppingzone.website.dto.CartDto;
import com.eshoppingzone.website.dto.UpdateCartItemRequest;
import com.eshoppingzone.website.session.SessionAuthHelper;
import com.eshoppingzone.website.session.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
public class CartWebController {

    private final CartClient cartClient;
    private final SessionAuthHelper authHelper;

    public CartWebController(CartClient cartClient, SessionAuthHelper authHelper) {
        this.cartClient = cartClient;
        this.authHelper = authHelper;
    }

    private boolean isMerchantUser(HttpSession session) {
        return authHelper.isMerchant(session);
    }

    @GetMapping
    public String viewCart(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/cart";
        }
        if (isMerchantUser(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Merchants cannot purchase products as customers.");
            return "redirect:/merchant/dashboard";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        try {
            CartDto cart = cartClient.getCartByCustomerId(currentUser.getProfileId());
            model.addAttribute("cart", cart);
            model.addAttribute("cartCount", cart != null ? cart.getTotalItemCount() : 0);
        } catch (Exception ex) {
            model.addAttribute("cart", new CartDto());
            model.addAttribute("cartCount", 0);
            model.addAttribute("errorMessage", "Unable to load cart. Cart service is currently unavailable.");
        }

        return "cart/cart";
    }

    @PostMapping("/add")
    public String addToCart(
            @RequestParam("productId") Long productId,
            @RequestParam(value = "quantity", defaultValue = "1") Integer quantity,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/products/" + productId;
        }
        if (isMerchantUser(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Merchants cannot add products to a shopping cart.");
            return "redirect:/merchant/dashboard";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);

        try {
            AddToCartRequest request = new AddToCartRequest(productId, quantity);
            cartClient.addItemToCart(currentUser.getProfileId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Item added to cart successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to add item to cart: " + ex.getMessage());
        }

        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String updateCartItem(
            @RequestParam("productId") Long productId,
            @RequestParam("quantity") Integer quantity,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/cart";
        }
        if (isMerchantUser(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Merchants cannot use customer cart operations.");
            return "redirect:/merchant/dashboard";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);

        try {
            UpdateCartItemRequest request = new UpdateCartItemRequest(quantity);
            cartClient.updateItemQuantity(currentUser.getProfileId(), productId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cart updated successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update cart: " + ex.getMessage());
        }

        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String removeFromCart(
            @RequestParam("productId") Long productId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/cart";
        }
        if (isMerchantUser(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Merchants cannot use customer cart operations.");
            return "redirect:/merchant/dashboard";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);

        try {
            cartClient.removeItemFromCart(currentUser.getProfileId(), productId);
            redirectAttributes.addFlashAttribute("successMessage", "Item removed from cart.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to remove item: " + ex.getMessage());
        }

        return "redirect:/cart";
    }
}
