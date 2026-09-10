package com.eshoppingzone.profile.dto;

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
public class OrderAssignedEvent implements Serializable {

    private Long orderId;
    private Long customerId;
    private Long deliveryAgentId;
    private String deliveryAgentName;
    private Double orderAmount;
    private String orderStatus;
    private List<Long> merchantIds;
    private LocalDateTime assignedDate;
}
