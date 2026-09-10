package com.eshoppingzone.website.client;

import com.eshoppingzone.website.dto.NotificationDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "profile-service-notifications", url = "${services.profile-service.url:http://localhost:8081}")
public interface NotificationClient {

    @GetMapping("/api/notifications/recipient/{recipientId}")
    List<NotificationDto> getNotificationsByRecipient(@PathVariable("recipientId") Long recipientId);

    @GetMapping("/api/notifications/recipient/{recipientId}/unread-count")
    Map<String, Object> getUnreadCount(@PathVariable("recipientId") Long recipientId);

    @PutMapping("/api/notifications/{id}/read")
    NotificationDto markAsRead(@PathVariable("id") Long id, @RequestParam(value = "recipientId", required = false) Long recipientId);

    @PutMapping("/api/notifications/recipient/{recipientId}/read-all")
    Map<String, String> markAllAsRead(@PathVariable("recipientId") Long recipientId);
}
