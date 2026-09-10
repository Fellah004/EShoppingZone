package com.eshoppingzone.cart.controller;

import com.eshoppingzone.cart.dto.AddToCartRequest;
import com.eshoppingzone.cart.dto.CartDto;
import com.eshoppingzone.cart.dto.UpdateCartItemRequest;
import com.eshoppingzone.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CartDto> getCartByCustomerId(@PathVariable("customerId") Long customerId) {
        return ResponseEntity.ok(cartService.getCartByCustomerId(customerId));
    }

    @GetMapping("/id/{cartId}")
    public ResponseEntity<CartDto> getCartById(@PathVariable("cartId") Long cartId) {
        return ResponseEntity.ok(cartService.getCartById(cartId));
    }

    @PostMapping("/{customerId}/items")
    public ResponseEntity<CartDto> addItemToCart(
            @PathVariable("customerId") Long customerId,
            @Valid @RequestBody AddToCartRequest request) {
        CartDto updated = cartService.addItemToCart(customerId, request);
        return new ResponseEntity<>(updated, HttpStatus.CREATED);
    }

    @PutMapping("/{customerId}/items/{productId}")
    public ResponseEntity<CartDto> updateItemQuantity(
            @PathVariable("customerId") Long customerId,
            @PathVariable("productId") Long productId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItemQuantity(customerId, productId, request));
    }

    @DeleteMapping("/{customerId}/items/{productId}")
    public ResponseEntity<CartDto> removeItemFromCart(
            @PathVariable("customerId") Long customerId,
            @PathVariable("productId") Long productId) {
        return ResponseEntity.ok(cartService.removeItemFromCart(customerId, productId));
    }

    @DeleteMapping("/{customerId}/clear")
    public ResponseEntity<CartDto> clearCart(@PathVariable("customerId") Long customerId) {
        return ResponseEntity.ok(cartService.clearCart(customerId));
    }
}
