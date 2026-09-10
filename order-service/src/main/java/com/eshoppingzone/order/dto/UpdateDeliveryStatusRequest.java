package com.eshoppingzone.order.dto;

import com.eshoppingzone.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDeliveryStatusRequest {

    @NotNull(message = "Delivery Agent ID is required")
    private Long deliveryAgentId;

    @NotNull(message = "New Order Status is required")
    private OrderStatus newStatus;
}
