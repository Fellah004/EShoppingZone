package com.eshoppingzone.website.client;

import com.eshoppingzone.website.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "profile-service", url = "${services.profile-service.url:http://localhost:8081}")
public interface ProfileClient {

    @PostMapping("/api/profile/register")
    AuthResponse register(@RequestBody RegisterRequest request);

    @PostMapping("/api/profile/login")
    AuthResponse login(@RequestBody LoginRequest request);

    @GetMapping("/api/profile/{id}")
    UserProfileDto getProfileById(@PathVariable("id") Long id);

    @GetMapping("/api/profile/email/{email}")
    UserProfileDto getProfileByEmail(@PathVariable("email") String email);

    @GetMapping("/api/profile/mobile/{mobileNumber}")
    UserProfileDto getProfileByMobileNumber(@PathVariable("mobileNumber") String mobileNumber);

    @PutMapping("/api/profile/{id}")
    UserProfileDto updateProfile(@PathVariable("id") Long id, @RequestBody UpdateProfileRequest request);

    @GetMapping("/api/profile/all")
    List<UserProfileDto> getAllProfiles();

    @GetMapping("/api/profile/role/{role}")
    List<UserProfileDto> getProfilesByRole(@PathVariable("role") String role);

    @PostMapping("/api/profile/delivery-agent")
    UserProfileDto createDeliveryAgent(@RequestBody RegisterRequest request);

    @PutMapping("/api/profile/{id}/toggle-status")
    UserProfileDto toggleStatus(@PathVariable("id") Long id);

    @PutMapping("/api/profile/{id}/availability")
    UserProfileDto updateAvailability(@PathVariable("id") Long id, @RequestParam("status") String status);

    @PutMapping("/api/profile/{id}/block")
    UserProfileDto blockUser(@PathVariable("id") Long id, @RequestParam("blocked") boolean blocked);

    @PutMapping("/api/profile/{id}/status")
    UserProfileDto updateAccountStatus(@PathVariable("id") Long id, @RequestParam("status") String status, @RequestParam(value = "reason", required = false) String reason);

    @PutMapping("/api/profile/{id}/zone")
    UserProfileDto assignDeliveryZone(@PathVariable("id") Long id, @RequestParam("zone") String zone);

    @GetMapping("/api/profile/customers/search")
    List<UserProfileDto> searchCustomers(@RequestParam(value = "query", required = false) String query);

    @PostMapping("/api/profile/forgot-password")
    java.util.Map<String, String> forgotPassword(@RequestBody ForgotPasswordRequest request);

    @PostMapping("/api/profile/reset-password")
    java.util.Map<String, String> resetPassword(@RequestBody ResetPasswordRequest request);

    @PutMapping("/api/profile/{id}/change-password")
    java.util.Map<String, String> changePassword(@PathVariable("id") Long id, @RequestBody ChangePasswordRequest request);

    @GetMapping("/api/profile/{profileId}/addresses")
    List<UserAddressDto> getAddresses(@PathVariable("profileId") Long profileId);

    @PostMapping("/api/profile/{profileId}/addresses")
    UserAddressDto addAddress(@PathVariable("profileId") Long profileId, @RequestBody CreateAddressRequest request);

    @PutMapping("/api/profile/{profileId}/addresses/{addressId}")
    UserAddressDto updateAddress(@PathVariable("profileId") Long profileId, @PathVariable("addressId") Long addressId, @RequestBody UpdateAddressRequest request);

    @DeleteMapping("/api/profile/{profileId}/addresses/{addressId}")
    void deleteAddress(@PathVariable("profileId") Long profileId, @PathVariable("addressId") Long addressId);

    @PutMapping("/api/profile/{profileId}/addresses/{addressId}/default")
    UserAddressDto setDefaultAddress(@PathVariable("profileId") Long profileId, @PathVariable("addressId") Long addressId);
}
