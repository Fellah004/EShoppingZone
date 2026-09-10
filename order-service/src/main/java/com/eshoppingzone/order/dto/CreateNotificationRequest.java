package com.eshoppingzone.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNotificationRequest {

    private Long recipientId;
    private String recipientRole;
    private String title;
    private String message;
    private String type;
}
