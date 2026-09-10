package com.eshoppingzone.profile.service;

import com.eshoppingzone.profile.dto.*;
import com.eshoppingzone.profile.entity.Role;

import java.util.List;

public interface UserProfileService {

    AuthResponse register(RegisterRequest request);

    AuthResponse authenticate(LoginRequest request);

    UserProfileDto getProfileById(Long id);

    UserProfileDto getProfileByEmail(String email);

    UserProfileDto getProfileByMobileNumber(String mobileNumber);

    UserProfileDto updateProfile(Long id, UpdateProfileRequest request);

    List<UserProfileDto> getAllProfiles();

    List<UserProfileDto> getProfilesByRole(Role role);

    UserProfileDto toggleStatus(Long id);

    UserProfileDto createDeliveryAgent(RegisterRequest request);

    String forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(Long id, ChangePasswordRequest request);

    List<UserAddressDto> getAddresses(Long profileId);

    UserAddressDto addAddress(Long profileId, CreateAddressRequest request);

    UserAddressDto updateAddress(Long profileId, Long addressId, UpdateAddressRequest request);

    void deleteAddress(Long profileId, Long addressId);

    UserAddressDto setDefaultAddress(Long profileId, Long addressId);

    UserProfileDto updateAvailability(Long id, String status);

    UserProfileDto blockUser(Long id, boolean blocked);

    UserProfileDto updateAccountStatus(Long id, String status, String reason);

    UserProfileDto assignDeliveryZone(Long id, String zone);

    List<UserProfileDto> searchCustomers(String query);
}
