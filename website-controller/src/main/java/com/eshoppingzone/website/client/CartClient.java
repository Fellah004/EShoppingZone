package com.eshoppingzone.website.client;

import com.eshoppingzone.website.dto.AddToCartRequest;
import com.eshoppingzone.website.dto.CartDto;
import com.eshoppingzone.website.dto.UpdateCartItemRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "cart-service", url = "${services.cart-service.url:http://localhost:8083}")
public interface CartClient {

    @GetMapping("/api/cart/{customerId}")
    CartDto getCartByCustomerId(@PathVariable("customerId") Long customerId);

    @PostMapping("/api/cart/{customerId}/items")
    CartDto addItemToCart(@PathVariable("customerId") Long customerId, @RequestBody AddToCartRequest request);

    @PutMapping("/api/cart/{customerId}/items/{productId}")
    CartDto updateItemQuantity(@PathVariable("customerId") Long customerId, @PathVariable("productId") Long productId, @RequestBody UpdateCartItemRequest request);

    @DeleteMapping("/api/cart/{customerId}/items/{productId}")
    CartDto removeItemFromCart(@PathVariable("customerId") Long customerId, @PathVariable("productId") Long productId);

    @DeleteMapping("/api/cart/{customerId}/clear")
    CartDto clearCart(@PathVariable("customerId") Long customerId);
}
