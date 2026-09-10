package com.eshoppingzone.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusUpdatedEvent implements Serializable {

    private Long orderId;
    private Long customerId;
    private Long deliveryAgentId;
    private String deliveryAgentName;
    private String previousStatus;
    private String newStatus;
    private Double orderAmount;
    private List<Long> merchantIds;
    private LocalDateTime updatedAt;
}
