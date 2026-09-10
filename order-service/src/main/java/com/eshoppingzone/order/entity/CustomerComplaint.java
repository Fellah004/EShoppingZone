package com.eshoppingzone.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_complaints")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerComplaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long complaintId;

    private Long orderId;

    private Long customerId;

    private String customerName;

    private Long merchantId;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private String complaintType = "COMPLAINT"; // COMPLAINT, DISPUTE

    @Column(nullable = false)
    @Builder.Default
    private String status = "OPEN"; // OPEN, IN_PROGRESS, RESOLVED, CLOSED

    @Column(columnDefinition = "TEXT")
    private String resolution;

    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "OPEN";
        }
        if (this.complaintType == null) {
            this.complaintType = "COMPLAINT";
        }
    }
}
