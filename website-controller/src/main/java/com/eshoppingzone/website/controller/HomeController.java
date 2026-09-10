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
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {

    private final ProductClient productClient;
    private final CartClient cartClient;
    private final SessionAuthHelper authHelper;

    public HomeController(ProductClient productClient, CartClient cartClient, SessionAuthHelper authHelper) {
        this.productClient = productClient;
        this.cartClient = cartClient;
        this.authHelper = authHelper;
    }

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        // Cart item count for badge
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

        // Featured products & categories
        List<ProductDto> products = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        try {
            products = productClient.getAllProducts();
            categories = productClient.getAllCategories();
        } catch (Exception ex) {
            model.addAttribute("serviceError", "Product service is currently unavailable. Please try again.");
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);

        return "index";
    }
}
