package com.eshoppingzone.profile.controller;

import com.eshoppingzone.profile.dto.CreateNotificationRequest;
import com.eshoppingzone.profile.dto.NotificationDto;
import com.eshoppingzone.profile.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<List<NotificationDto>> getNotificationsByRecipient(@PathVariable("recipientId") Long recipientId) {
        return ResponseEntity.ok(notificationService.getNotificationsByRecipient(recipientId));
    }

    @GetMapping("/recipient/{recipientId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable("recipientId") Long recipientId) {
        long unreadCount = notificationService.getUnreadCount(recipientId);
        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationDto> markAsRead(
            @PathVariable("id") Long id,
            @RequestParam(value = "recipientId", required = false) Long recipientId) {
        return ResponseEntity.ok(notificationService.markAsRead(id, recipientId));
    }

    @PutMapping("/recipient/{recipientId}/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(@PathVariable("recipientId") Long recipientId) {
        notificationService.markAllAsRead(recipientId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @PostMapping
    public ResponseEntity<NotificationDto> createNotification(@Valid @RequestBody CreateNotificationRequest request) {
        NotificationDto created = notificationService.createNotification(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }
}
