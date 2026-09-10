package com.eshoppingzone.cart.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartId;

    @Column(nullable = false, unique = true)
    private Long customerId;

    @Column(nullable = false)
    private Double totalPrice;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    public void recalculateTotal() {
        if (items == null || items.isEmpty()) {
            this.totalPrice = 0.0;
        } else {
            this.totalPrice = items.stream()
                    .mapToDouble(CartItem::getSubTotal)
                    .sum();
        }
    }
}
