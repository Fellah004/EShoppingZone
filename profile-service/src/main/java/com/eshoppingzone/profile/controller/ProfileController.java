package com.eshoppingzone.profile.controller;

import com.eshoppingzone.profile.dto.*;
import com.eshoppingzone.profile.entity.Role;
import com.eshoppingzone.profile.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/profile", "/api/profiles"})
public class ProfileController {

    private final UserProfileService profileService;

    public ProfileController(UserProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = profileService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = profileService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileDto> getProfileById(@PathVariable("id") Long id) {
        UserProfileDto profile = profileService.getProfileById(id);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserProfileDto> getProfileByEmail(@PathVariable("email") String email) {
        UserProfileDto profile = profileService.getProfileByEmail(email);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/mobile/{mobileNumber}")
    public ResponseEntity<UserProfileDto> getProfileByMobileNumber(@PathVariable("mobileNumber") String mobileNumber) {
        UserProfileDto profile = profileService.getProfileByMobileNumber(mobileNumber);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserProfileDto> updateProfile(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileDto updated = profileService.updateProfile(id, request);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserProfileDto>> getAllProfiles() {
        return ResponseEntity.ok(profileService.getAllProfiles());
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserProfileDto>> getProfilesByRole(@PathVariable("role") Role role) {
        return ResponseEntity.ok(profileService.getProfilesByRole(role));
    }

    @PostMapping({"/delivery-agent", "/delivery-agents"})
    public ResponseEntity<UserProfileDto> createDeliveryAgent(@Valid @RequestBody RegisterRequest request) {
        UserProfileDto created = profileService.createDeliveryAgent(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping({"/{id}/toggle-status", "/delivery-agents/{id}/toggle-status", "/delivery-agent/{id}/toggle-status"})
    public ResponseEntity<UserProfileDto> toggleStatus(@PathVariable("id") Long id) {
        return ResponseEntity.ok(profileService.toggleStatus(id));
    }

    @PutMapping({"/{id}/availability", "/delivery-agents/{id}/availability", "/delivery-agent/{id}/availability"})
    public ResponseEntity<UserProfileDto> updateAvailability(
            @PathVariable("id") Long id,
            @RequestParam("status") String status) {
        return ResponseEntity.ok(profileService.updateAvailability(id, status));
    }

    @PutMapping("/{id}/block")
    public ResponseEntity<UserProfileDto> blockUser(
            @PathVariable("id") Long id,
            @RequestParam("blocked") boolean blocked) {
        return ResponseEntity.ok(profileService.blockUser(id, blocked));
    }

    @PutMapping({"/{id}/account-status", "/{id}/status"})
    public ResponseEntity<UserProfileDto> updateAccountStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            @RequestParam(value = "reason", required = false) String reason) {
        return ResponseEntity.ok(profileService.updateAccountStatus(id, status, reason));
    }

    @PutMapping("/{id}/zone")
    public ResponseEntity<UserProfileDto> assignDeliveryZone(
            @PathVariable("id") Long id,
            @RequestParam("zone") String zone) {
        return ResponseEntity.ok(profileService.assignDeliveryZone(id, zone));
    }

    @GetMapping("/customers/search")
    public ResponseEntity<List<UserProfileDto>> searchCustomers(
            @RequestParam(value = "query", required = false) String query) {
        return ResponseEntity.ok(profileService.searchCustomers(query));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<java.util.Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String token = profileService.forgotPassword(request);
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("message", "If an account with that email exists, password reset instructions have been generated.");
        response.put("token", token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<java.util.Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        profileService.resetPassword(request);
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("message", "Password has been reset successfully.");
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/{id}/change-password", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<java.util.Map<String, String>> changePassword(
            @PathVariable("id") Long id,
            @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(id, request);
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("message", "Password changed successfully.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{profileId}/addresses")
    public ResponseEntity<List<UserAddressDto>> getAddresses(@PathVariable("profileId") Long profileId) {
        return ResponseEntity.ok(profileService.getAddresses(profileId));
    }

    @PostMapping("/{profileId}/addresses")
    public ResponseEntity<UserAddressDto> addAddress(
            @PathVariable("profileId") Long profileId,
            @Valid @RequestBody CreateAddressRequest request) {
        UserAddressDto created = profileService.addAddress(profileId, request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{profileId}/addresses/{addressId}")
    public ResponseEntity<UserAddressDto> updateAddress(
            @PathVariable("profileId") Long profileId,
            @PathVariable("addressId") Long addressId,
            @Valid @RequestBody UpdateAddressRequest request) {
        return ResponseEntity.ok(profileService.updateAddress(profileId, addressId, request));
    }

    @DeleteMapping("/{profileId}/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable("profileId") Long profileId,
            @PathVariable("addressId") Long addressId) {
        profileService.deleteAddress(profileId, addressId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{profileId}/addresses/{addressId}/default")
    public ResponseEntity<UserAddressDto> setDefaultAddress(
            @PathVariable("profileId") Long profileId,
            @PathVariable("addressId") Long addressId) {
        return ResponseEntity.ok(profileService.setDefaultAddress(profileId, addressId));
    }
}
