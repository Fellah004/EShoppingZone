package com.eshoppingzone.cart.service;

import com.eshoppingzone.cart.dto.AddToCartRequest;
import com.eshoppingzone.cart.dto.CartDto;
import com.eshoppingzone.cart.dto.UpdateCartItemRequest;

public interface CartService {

    CartDto getCartByCustomerId(Long customerId);

    CartDto getCartById(Long cartId);

    CartDto addItemToCart(Long customerId, AddToCartRequest request);

    CartDto updateItemQuantity(Long customerId, Long productId, UpdateCartItemRequest request);

    CartDto removeItemFromCart(Long customerId, Long productId);

    CartDto clearCart(Long customerId);
}
