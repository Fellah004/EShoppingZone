package com.eshoppingzone.cart.service;

import com.eshoppingzone.cart.client.ProductClient;
import com.eshoppingzone.cart.dto.*;
import com.eshoppingzone.cart.entity.Cart;
import com.eshoppingzone.cart.entity.CartItem;
import com.eshoppingzone.cart.exception.ResourceNotFoundException;
import com.eshoppingzone.cart.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductClient productClient;

    public CartServiceImpl(CartRepository cartRepository, ProductClient productClient) {
        this.cartRepository = cartRepository;
        this.productClient = productClient;
    }

    @Override
    public CartDto getCartByCustomerId(Long customerId) {
        Cart cart = getOrCreateCart(customerId);
        return mapToDto(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartDto getCartById(Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with id: " + cartId));
        return mapToDto(cart);
    }

    @Override
    public CartDto addItemToCart(Long customerId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(customerId);

        // Fetch fresh product info from Product Service via Feign
        ProductDto product = productClient.getProductById(request.getProductId());
        if (product == null) {
            throw new ResourceNotFoundException("Product not found with id: " + request.getProductId());
        }

        if (Boolean.FALSE.equals(product.getInStock()) || (product.getStockQuantity() != null && product.getStockQuantity() <= 0)) {
            throw new IllegalArgumentException("Product '" + product.getProductName() + "' is currently out of stock.");
        }

        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            CartItem existing = existingItemOpt.get();
            int newQuantity = existing.getQuantity() + request.getQuantity();
            if (product.getStockQuantity() != null && newQuantity > product.getStockQuantity()) {
                throw new IllegalArgumentException("Requested quantity exceeds available stock (" + product.getStockQuantity() + " units available).");
            }
            existing.setQuantity(newQuantity);
            double effectivePrice = product.getEffectivePrice();
            existing.setPrice(effectivePrice);
            existing.setSubTotal(effectivePrice * newQuantity);
            if (product.getMerchantId() != null) {
                existing.setMerchantId(product.getMerchantId());
            }
        } else {
            double effectivePrice = product.getEffectivePrice();
            CartItem newItem = new CartItem();
            newItem.setProductId(product.getProductId());
            newItem.setMerchantId(product.getMerchantId());
            newItem.setProductName(product.getProductName());
            newItem.setPrice(effectivePrice);
            newItem.setQuantity(request.getQuantity());
            newItem.setSubTotal(effectivePrice * request.getQuantity());
            newItem.setImageUrl(product.getImage());
            newItem.setCart(cart);
            cart.getItems().add(newItem);
        }

        cart.recalculateTotal();
        Cart saved = cartRepository.save(cart);
        return mapToDto(saved);
    }

    @Override
    public CartDto updateItemQuantity(Long customerId, Long productId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(customerId);

        if (request.getQuantity() <= 0) {
            cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        } else {
            Optional<CartItem> itemOpt = cart.getItems().stream()
                    .filter(item -> item.getProductId().equals(productId))
                    .findFirst();

            if (itemOpt.isPresent()) {
                CartItem item = itemOpt.get();
                item.setQuantity(request.getQuantity());
                item.setSubTotal(item.getPrice() * request.getQuantity());
            } else {
                // If item not found in cart, fetch and add
                ProductDto product = productClient.getProductById(productId);
                double effectivePrice = product != null ? product.getEffectivePrice() : 0.0;
                CartItem newItem = new CartItem();
                newItem.setProductId(product.getProductId());
                newItem.setMerchantId(product.getMerchantId());
                newItem.setProductName(product.getProductName());
                newItem.setPrice(effectivePrice);
                newItem.setQuantity(request.getQuantity());
                newItem.setSubTotal(effectivePrice * request.getQuantity());
                newItem.setImageUrl(product.getImage());
                newItem.setCart(cart);
                cart.getItems().add(newItem);
            }
        }

        cart.recalculateTotal();
        Cart saved = cartRepository.save(cart);
        return mapToDto(saved);
    }

    @Override
    public CartDto removeItemFromCart(Long customerId, Long productId) {
        Cart cart = getOrCreateCart(customerId);
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        cart.recalculateTotal();
        Cart saved = cartRepository.save(cart);
        return mapToDto(saved);
    }

    @Override
    public CartDto clearCart(Long customerId) {
        Cart cart = getOrCreateCart(customerId);
        cart.getItems().clear();
        cart.setTotalPrice(0.0);
        Cart saved = cartRepository.save(cart);
        return mapToDto(saved);
    }

    private Cart getOrCreateCart(Long customerId) {
        return cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setCustomerId(customerId);
                    newCart.setTotalPrice(0.0);
                    newCart.setItems(new ArrayList<>());
                    return cartRepository.save(newCart);
                });
    }

    private CartDto mapToDto(Cart cart) {
        var itemDtos = cart.getItems().stream()
                .map(item -> new CartItemDto(
                        item.getCartItemId(),
                        item.getProductId(),
                        item.getMerchantId(),
                        item.getProductName(),
                        item.getPrice(),
                        item.getQuantity(),
                        item.getSubTotal(),
                        item.getImageUrl()
                ))
                .collect(Collectors.toList());

        return new CartDto(
                cart.getCartId(),
                cart.getCustomerId(),
                cart.getTotalPrice(),
                itemDtos
        );
    }
}
