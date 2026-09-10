package com.eshoppingzone.order.client;

import com.eshoppingzone.order.dto.CreateNotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "profile-service-notifications", url = "${services.profile-service.url:http://localhost:8081}")
public interface NotificationClient {

    @PostMapping("/api/notifications")
    Map<String, Object> createNotification(@RequestBody CreateNotificationRequest request);
}
