package com.eshoppingzone.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service", url = "${services.product-service.url:http://localhost:8082}")
public interface ProductClient {

    @PutMapping("/api/products/{id}/add-stock")
    Object addStock(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity);

    @PutMapping("/api/products/{id}/deduct-stock")
    Object deductStock(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity);

    @PutMapping("/api/products/{id}/restock")
    Object restock(@PathVariable("id") Long id, @RequestParam(value = "quantity", required = false) Integer quantity);
}
