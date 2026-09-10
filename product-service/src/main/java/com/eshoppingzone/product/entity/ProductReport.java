package com.eshoppingzone.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @Column(nullable = false)
    private String targetType; // 'PRODUCT' or 'REVIEW'

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(nullable = false)
    private LocalDateTime reportDate;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";
}
