package com.eshoppingzone.website.controller;

import com.eshoppingzone.website.client.CartClient;
import com.eshoppingzone.website.client.WalletClient;
import com.eshoppingzone.website.dto.AddMoneyRequest;
import com.eshoppingzone.website.dto.CartDto;
import com.eshoppingzone.website.dto.TransactionDto;
import com.eshoppingzone.website.dto.WalletDto;
import com.eshoppingzone.website.session.SessionAuthHelper;
import com.eshoppingzone.website.session.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/wallet")
public class WalletWebController {

    private final WalletClient walletClient;
    private final CartClient cartClient;
    private final SessionAuthHelper authHelper;

    public WalletWebController(WalletClient walletClient, CartClient cartClient, SessionAuthHelper authHelper) {
        this.walletClient = walletClient;
        this.cartClient = cartClient;
        this.authHelper = authHelper;
    }

    private void populateCartCount(UserSession user, Model model) {
        int cartCount = 0;
        if (user != null && user.isLoggedIn() && user.isCustomer()) {
            try {
                CartDto cart = cartClient.getCartByCustomerId(user.getProfileId());
                if (cart != null) {
                    cartCount = cart.getTotalItemCount();
                }
            } catch (Exception ignored) {
            }
        }
        model.addAttribute("cartCount", cartCount);
    }

    @GetMapping
    public String viewWallet(
            @RequestParam(value = "success", required = false) String success,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/wallet";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);
        if (currentUser.isMerchant()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Merchants cannot use the customer digital wallet.");
            return "redirect:/merchant/dashboard";
        }

        model.addAttribute("currentUser", currentUser);
        populateCartCount(currentUser, model);

        if (success != null) {
            model.addAttribute("successMessage", "Funds added to your wallet successfully!");
        }

        try {
            WalletDto wallet = walletClient.getWallet(currentUser.getProfileId());
            model.addAttribute("wallet", wallet);
        } catch (Exception ex) {
            model.addAttribute("wallet", new WalletDto(null, currentUser.getProfileId(), 0.0));
            model.addAttribute("errorMessage", "Unable to retrieve wallet balance.");
        }

        return "wallet/wallet";
    }

    @PostMapping("/add-money")
    public String addMoney(
            @RequestParam("amount") Double amount,
            @RequestParam(value = "remarks", defaultValue = "Wallet Top-up") String remarks,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/wallet";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);
        if (currentUser.isMerchant()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Merchants cannot use the customer digital wallet.");
            return "redirect:/merchant/dashboard";
        }

        if (amount == null || amount <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please enter a valid amount greater than zero.");
            return "redirect:/wallet";
        }

        try {
            AddMoneyRequest request = new AddMoneyRequest(currentUser.getProfileId(), amount, remarks);
            walletClient.addMoney(request);
            redirectAttributes.addFlashAttribute("successMessage", String.format("₹%.2f added to your wallet successfully!", amount));
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to add money: " + ex.getMessage());
        }

        return "redirect:/wallet";
    }

    @GetMapping("/transactions")
    public String transactionHistory(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/wallet/transactions";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);
        if (currentUser.isMerchant()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Merchants cannot use customer wallet transactions.");
            return "redirect:/merchant/dashboard";
        }

        model.addAttribute("currentUser", currentUser);
        populateCartCount(currentUser, model);

        List<TransactionDto> allTransactions = new ArrayList<>();
        WalletDto wallet = null;
        double currentBalance = 0.0;
        try {
            wallet = walletClient.getWallet(currentUser.getProfileId());
            if (wallet != null) {
                currentBalance = wallet.getCurrentBalance() != null ? wallet.getCurrentBalance() : 0.0;
            }
            allTransactions = walletClient.getTransactions(currentUser.getProfileId());
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Unable to load transaction statements.");
        }

        if (wallet == null) {
            wallet = new WalletDto(null, currentUser.getProfileId(), currentBalance);
        }

        com.eshoppingzone.website.dto.PagedResult<TransactionDto> pageData = com.eshoppingzone.website.dto.PagedResult.from(allTransactions, page, size);

        model.addAttribute("wallet", wallet);
        model.addAttribute("walletBalance", currentBalance);
        model.addAttribute("transactions", pageData.getContent());
        model.addAttribute("pageData", pageData);
        model.addAttribute("baseUrl", "/wallet/transactions");
        return "wallet/transactions";
    }
}
