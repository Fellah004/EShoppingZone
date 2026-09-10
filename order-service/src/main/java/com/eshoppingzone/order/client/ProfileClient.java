package com.eshoppingzone.order.client;

import com.eshoppingzone.order.dto.UserProfileDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "profile-service-users", url = "${services.profile-service.url:http://localhost:8081}")
public interface ProfileClient {

    @GetMapping("/api/profile/{id}")
    UserProfileDto getProfile(@PathVariable("id") Long id);

    @GetMapping("/api/profiles/role/{role}")
    java.util.List<UserProfileDto> getProfilesByRole(@PathVariable("role") String role);
}
