package com.eshoppingzone.profile.service;

import com.eshoppingzone.profile.dto.CreateNotificationRequest;
import com.eshoppingzone.profile.dto.NotificationDto;

import java.util.List;

public interface NotificationService {
    List<NotificationDto> getNotificationsByRecipient(Long recipientId);
    long getUnreadCount(Long recipientId);
    NotificationDto markAsRead(Long notificationId, Long recipientId);
    void markAllAsRead(Long recipientId);
    NotificationDto createNotification(CreateNotificationRequest request);
}
