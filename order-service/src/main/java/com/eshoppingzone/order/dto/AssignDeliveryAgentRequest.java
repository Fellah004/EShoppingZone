package com.eshoppingzone.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignDeliveryAgentRequest {

    @NotNull(message = "Delivery Agent ID is required")
    private Long deliveryAgentId;

    @NotBlank(message = "Delivery Agent name is required")
    private String deliveryAgentName;
}
