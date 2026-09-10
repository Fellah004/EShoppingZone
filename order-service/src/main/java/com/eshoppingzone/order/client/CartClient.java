package com.eshoppingzone.order.client;

import com.eshoppingzone.order.dto.CartDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "cart-service", url = "${services.cart-service.url:http://localhost:8083}")
public interface CartClient {

    @GetMapping("/api/cart/{customerId}")
    CartDto getCartByCustomerId(@PathVariable("customerId") Long customerId);

    @DeleteMapping("/api/cart/{customerId}/clear")
    CartDto clearCart(@PathVariable("customerId") Long customerId);
}
