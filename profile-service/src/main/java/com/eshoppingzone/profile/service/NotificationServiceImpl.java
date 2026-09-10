package com.eshoppingzone.profile.service;

import com.eshoppingzone.profile.dto.CreateNotificationRequest;
import com.eshoppingzone.profile.dto.NotificationDto;
import com.eshoppingzone.profile.entity.Notification;
import com.eshoppingzone.profile.exception.ResourceNotFoundException;
import com.eshoppingzone.profile.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getNotificationsByRecipient(Long recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long recipientId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(recipientId);
    }

    @Override
    public NotificationDto markAsRead(Long notificationId, Long recipientId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (recipientId != null && !notification.getRecipientId().equals(recipientId)) {
            throw new IllegalArgumentException("Unauthorized to modify this notification");
        }

        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);
        return mapToDto(saved);
    }

    @Override
    public void markAllAsRead(Long recipientId) {
        List<Notification> unreadList = notificationRepository.findByRecipientIdAndIsReadFalse(recipientId);
        for (Notification notification : unreadList) {
            notification.setIsRead(true);
        }
        notificationRepository.saveAll(unreadList);
    }

    @Override
    public NotificationDto createNotification(CreateNotificationRequest request) {
        Notification notification = new Notification();
        notification.setRecipientId(request.getRecipientId());
        notification.setRecipientRole(request.getRecipientRole());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setType(request.getType());
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        Notification saved = notificationRepository.save(notification);
        return mapToDto(saved);
    }

    private NotificationDto mapToDto(Notification notification) {
        return new NotificationDto(
                notification.getNotificationId(),
                notification.getRecipientId(),
                notification.getRecipientRole(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getIsRead(),
                notification.getCreatedAt()
        );
    }
}
