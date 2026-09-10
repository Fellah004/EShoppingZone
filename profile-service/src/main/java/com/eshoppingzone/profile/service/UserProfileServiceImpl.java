package com.eshoppingzone.profile.service;

import com.eshoppingzone.profile.dto.*;
import com.eshoppingzone.profile.entity.PasswordResetToken;
import com.eshoppingzone.profile.entity.Role;
import com.eshoppingzone.profile.entity.UserAddress;
import com.eshoppingzone.profile.entity.UserProfile;
import com.eshoppingzone.profile.exception.InvalidCredentialsException;
import com.eshoppingzone.profile.exception.ResourceNotFoundException;
import com.eshoppingzone.profile.exception.UserAlreadyExistsException;
import com.eshoppingzone.profile.repository.PasswordResetTokenRepository;
import com.eshoppingzone.profile.repository.UserAddressRepository;
import com.eshoppingzone.profile.repository.UserProfileRepository;
import com.eshoppingzone.profile.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final UserAddressRepository addressRepository;
    private final NotificationService notificationService;

    public UserProfileServiceImpl(
            UserProfileRepository profileRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            PasswordResetTokenRepository resetTokenRepository,
            UserAddressRepository addressRepository,
            NotificationService notificationService) {
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.resetTokenRepository = resetTokenRepository;
        this.addressRepository = addressRepository;
        this.notificationService = notificationService;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (profileRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("A user with email " + request.getEmail() + " already exists");
        }

        if (profileRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new UserAlreadyExistsException("A user with mobile number " + request.getMobileNumber() + " already exists");
        }

        UserProfile userProfile = new UserProfile();
        userProfile.setFullName(request.getFullName());
        if (request.getBusinessName() != null) {
            userProfile.setBusinessName(request.getBusinessName());
        }
        userProfile.setEmail(request.getEmail());
        userProfile.setPassword(passwordEncoder.encode(request.getPassword()));
        userProfile.setMobileNumber(request.getMobileNumber());
        userProfile.setAbout(request.getAbout());
        userProfile.setDateOfBirth(request.getDateOfBirth());
        userProfile.setGender(request.getGender());
        userProfile.setRole(request.getRole() != null ? request.getRole() : Role.CUSTOMER);
        userProfile.setProfileImage(request.getProfileImage());
        userProfile.setAddress(request.getAddress());
        userProfile.setVehicleType(request.getVehicleType());
        userProfile.setVehicleNumber(request.getVehicleNumber());
        userProfile.setVehicleModel(request.getVehicleModel());
        userProfile.setDrivingLicenseNumber(request.getDrivingLicenseNumber());
        userProfile.setDrivingLicenseUrl(request.getDrivingLicenseUrl());
        userProfile.setVehicleRegistrationUrl(request.getVehicleRegistrationUrl());
        userProfile.setIdentityDocumentUrl(request.getIdentityDocumentUrl());
        userProfile.setAvailability(request.getAvailability() != null ? request.getAvailability() : "AVAILABLE");
        userProfile.setActive(true);

        UserProfile saved = profileRepository.save(userProfile);

        String token = jwtTokenProvider.generateToken(
                saved.getProfileId(),
                saved.getEmail(),
                saved.getRole(),
                saved.getFullName()
        );

        return new AuthResponse(
                token,
                "Bearer",
                saved.getProfileId(),
                saved.getFullName(),
                saved.getEmail(),
                saved.getRole(),
                saved.getMobileNumber()
        );
    }

    @Override
    public AuthResponse authenticate(LoginRequest request) {
        UserProfile userProfile = profileRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), userProfile.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (Boolean.TRUE.equals(userProfile.getBlocked())) {
            throw new InvalidCredentialsException("Account is blocked by administrator. Please contact support.");
        }
        if ("SUSPENDED".equalsIgnoreCase(userProfile.getAccountStatus())) {
            throw new InvalidCredentialsException("Account is suspended by administrator.");
        }
        if (!Boolean.TRUE.equals(userProfile.getActive())) {
            throw new InvalidCredentialsException("Account is deactivated. Please contact support.");
        }

        String token = jwtTokenProvider.generateToken(
                userProfile.getProfileId(),
                userProfile.getEmail(),
                userProfile.getRole(),
                userProfile.getFullName()
        );

        return new AuthResponse(
                token,
                "Bearer",
                userProfile.getProfileId(),
                userProfile.getFullName(),
                userProfile.getEmail(),
                userProfile.getRole(),
                userProfile.getMobileNumber()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getProfileById(Long id) {
        UserProfile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with id: " + id));
        return mapToDto(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getProfileByEmail(String email) {
        UserProfile profile = profileRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with email: " + email));
        return mapToDto(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getProfileByMobileNumber(String mobileNumber) {
        UserProfile profile = profileRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with mobile number: " + mobileNumber));
        return mapToDto(profile);
    }

    @Override
    public UserProfileDto updateProfile(Long id, UpdateProfileRequest request) {
        UserProfile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with id: " + id));

        profile.setFullName(request.getFullName());
        if (request.getBusinessName() != null) {
            profile.setBusinessName(request.getBusinessName());
        }
        profile.setMobileNumber(request.getMobileNumber());
        if (request.getAbout() != null) {
            profile.setAbout(request.getAbout());
        }
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());
        if (request.getProfileImage() != null) {
            profile.setProfileImage(request.getProfileImage());
        }
        if (request.getAddress() != null) {
            profile.setAddress(request.getAddress());
        }
        if (request.getVehicleType() != null) {
            profile.setVehicleType(request.getVehicleType());
        }
        if (request.getVehicleNumber() != null) {
            profile.setVehicleNumber(request.getVehicleNumber());
        }
        if (request.getVehicleModel() != null) {
            profile.setVehicleModel(request.getVehicleModel());
        }
        if (request.getDrivingLicenseNumber() != null) {
            profile.setDrivingLicenseNumber(request.getDrivingLicenseNumber());
        }
        if (request.getDrivingLicenseUrl() != null) {
            profile.setDrivingLicenseUrl(request.getDrivingLicenseUrl());
        }
        if (request.getVehicleRegistrationUrl() != null) {
            profile.setVehicleRegistrationUrl(request.getVehicleRegistrationUrl());
        }
        if (request.getIdentityDocumentUrl() != null) {
            profile.setIdentityDocumentUrl(request.getIdentityDocumentUrl());
        }
        if (request.getAvailability() != null) {
            profile.setAvailability(request.getAvailability());
        }

        UserProfile updated = profileRepository.save(profile);
        return mapToDto(updated);
    }

    @Override
    public UserProfileDto updateAvailability(Long id, String status) {
        UserProfile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with id: " + id));

        if (status == null || (!"AVAILABLE".equalsIgnoreCase(status) && !"BUSY".equalsIgnoreCase(status) && !"OFFLINE".equalsIgnoreCase(status))) {
            throw new IllegalArgumentException("Invalid availability status: " + status + ". Allowed values: AVAILABLE, BUSY, OFFLINE");
        }
        profile.setAvailability(status.toUpperCase());
        UserProfile saved = profileRepository.save(profile);
        return mapToDto(saved);
    }

    @Override
    public UserProfileDto toggleStatus(Long id) {
        UserProfile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with id: " + id));

        boolean currentActive = profile.getActive() != null ? profile.getActive() : true;
        profile.setActive(!currentActive);
        UserProfile saved = profileRepository.save(profile);
        return mapToDto(saved);
    }

    @Override
    public UserProfileDto createDeliveryAgent(RegisterRequest request) {
        if (profileRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("A user with email " + request.getEmail() + " already exists");
        }

        if (profileRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new UserAlreadyExistsException("A user with mobile number " + request.getMobileNumber() + " already exists");
        }

        UserProfile userProfile = new UserProfile();
        userProfile.setFullName(request.getFullName());
        userProfile.setEmail(request.getEmail());
        userProfile.setPassword(passwordEncoder.encode(request.getPassword()));
        userProfile.setMobileNumber(request.getMobileNumber());
        userProfile.setAbout(request.getAbout());
        userProfile.setDateOfBirth(request.getDateOfBirth());
        userProfile.setGender(request.getGender());
        userProfile.setRole(Role.DELIVERY_AGENT);
        userProfile.setActive(true);
        userProfile.setProfileImage(request.getProfileImage());
        userProfile.setAddress(request.getAddress());
        userProfile.setVehicleType(request.getVehicleType());
        userProfile.setVehicleNumber(request.getVehicleNumber());
        userProfile.setVehicleModel(request.getVehicleModel());
        userProfile.setDrivingLicenseNumber(request.getDrivingLicenseNumber());
        userProfile.setDrivingLicenseUrl(request.getDrivingLicenseUrl());
        userProfile.setVehicleRegistrationUrl(request.getVehicleRegistrationUrl());
        userProfile.setIdentityDocumentUrl(request.getIdentityDocumentUrl());
        userProfile.setAvailability(request.getAvailability() != null ? request.getAvailability() : "AVAILABLE");

        UserProfile saved = profileRepository.save(userProfile);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileDto> getAllProfiles() {
        return profileRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileDto> getProfilesByRole(Role role) {
        return profileRepository.findByRole(role).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        Optional<UserProfile> userOpt = profileRepository.findByEmail(request.getEmail());
        if (userOpt.isPresent()) {
            UserProfile user = userOpt.get();
            // Invalidate existing tokens for this email
            resetTokenRepository.deleteByEmail(user.getEmail());

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .email(user.getEmail())
                    .expiryDate(LocalDateTime.now().plusMinutes(30))
                    .build();
            resetTokenRepository.save(resetToken);

            // Send notification
            try {
                notificationService.createNotification(new CreateNotificationRequest(
                        user.getProfileId(),
                        user.getRole().name(),
                        "Password Reset Instructions",
                        "Use this reset token to reset your password within 30 minutes: " + token,
                        "PASSWORD_RESET"
                ));
            } catch (Exception ignored) {
            }
            return token;
        }
        // Return null or simulated token without error to prevent account enumeration
        return UUID.randomUUID().toString();
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired password reset token"));

        if (resetToken.isExpired()) {
            resetTokenRepository.delete(resetToken);
            throw new InvalidCredentialsException("Password reset token has expired. Please request a new one.");
        }

        UserProfile user = profileRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found for this reset token"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        profileRepository.save(user);

        resetTokenRepository.delete(resetToken);

        try {
            notificationService.createNotification(new CreateNotificationRequest(
                    user.getProfileId(),
                    user.getRole().name(),
                    "Password Changed Successfully",
                    "Your password has been successfully updated.",
                    "SECURITY_ALERT"
            ));
        } catch (Exception ignored) {
        }
    }

    @Override
    @Transactional
    public void changePassword(Long id, ChangePasswordRequest request) {
        UserProfile user = profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password does not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        profileRepository.save(user);

        try {
            notificationService.createNotification(new CreateNotificationRequest(
                    user.getProfileId(),
                    user.getRole().name(),
                    "Password Changed Successfully",
                    "Your password has been successfully changed.",
                    "SECURITY_ALERT"
            ));
        } catch (Exception ignored) {
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAddressDto> getAddresses(Long profileId) {
        return addressRepository.findByProfileId(profileId).stream()
                .map(this::mapToAddressDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserAddressDto addAddress(Long profileId, CreateAddressRequest request) {
        if (!profileRepository.existsById(profileId)) {
            throw new ResourceNotFoundException("User profile not found with id: " + profileId);
        }

        List<UserAddress> existing = addressRepository.findByProfileId(profileId);
        boolean isDefault = Boolean.TRUE.equals(request.getIsDefault()) || existing.isEmpty();

        if (isDefault) {
            existing.forEach(a -> {
                a.setIsDefault(false);
                addressRepository.save(a);
            });
        }

        UserAddress address = UserAddress.builder()
                .profileId(profileId)
                .fullName(request.getFullName())
                .mobileNumber(request.getMobileNumber())
                .houseNumber(request.getHouseNumber())
                .streetName(request.getStreetName())
                .colonyName(request.getColonyName())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .isDefault(isDefault)
                .build();

        UserAddress saved = addressRepository.save(address);
        return mapToAddressDto(saved);
    }

    @Override
    public UserAddressDto updateAddress(Long profileId, Long addressId, UpdateAddressRequest request) {
        UserAddress address = addressRepository.findByAddressIdAndProfileId(addressId, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId + " for profile: " + profileId));

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            List<UserAddress> existing = addressRepository.findByProfileId(profileId);
            existing.forEach(a -> {
                if (!a.getAddressId().equals(addressId)) {
                    a.setIsDefault(false);
                    addressRepository.save(a);
                }
            });
            address.setIsDefault(true);
        } else if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }

        address.setFullName(request.getFullName());
        address.setMobileNumber(request.getMobileNumber());
        address.setHouseNumber(request.getHouseNumber());
        address.setStreetName(request.getStreetName());
        address.setColonyName(request.getColonyName());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());

        UserAddress updated = addressRepository.save(address);
        return mapToAddressDto(updated);
    }

    @Override
    public void deleteAddress(Long profileId, Long addressId) {
        UserAddress address = addressRepository.findByAddressIdAndProfileId(addressId, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));
        addressRepository.delete(address);
    }

    @Override
    public UserAddressDto setDefaultAddress(Long profileId, Long addressId) {
        UserAddress address = addressRepository.findByAddressIdAndProfileId(addressId, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        List<UserAddress> existing = addressRepository.findByProfileId(profileId);
        existing.forEach(a -> {
            a.setIsDefault(a.getAddressId().equals(addressId));
            addressRepository.save(a);
        });

        address.setIsDefault(true);
        return mapToAddressDto(address);
    }

    private UserAddressDto mapToAddressDto(UserAddress address) {
        return UserAddressDto.builder()
                .addressId(address.getAddressId())
                .profileId(address.getProfileId())
                .fullName(address.getFullName())
                .mobileNumber(address.getMobileNumber())
                .houseNumber(address.getHouseNumber())
                .streetName(address.getStreetName())
                .colonyName(address.getColonyName())
                .city(address.getCity())
                .state(address.getState())
                .pincode(address.getPincode())
                .isDefault(address.getIsDefault())
                .build();
    }

    @Override
    @Transactional
    public UserProfileDto blockUser(Long id, boolean blocked) {
        UserProfile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with id: " + id));
        profile.setBlocked(blocked);
        if (blocked) {
            profile.setAccountStatus("BLOCKED");
        } else {
            profile.setAccountStatus("APPROVED");
        }
        UserProfile saved = profileRepository.save(profile);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public UserProfileDto updateAccountStatus(Long id, String status, String reason) {
        UserProfile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with id: " + id));
        profile.setAccountStatus(status);
        if (reason != null && !reason.isBlank()) {
            profile.setRejectionReason(reason);
        }
        if ("APPROVED".equalsIgnoreCase(status) || "ACTIVE".equalsIgnoreCase(status)) {
            profile.setActive(true);
            profile.setBlocked(false);
        } else if ("REJECTED".equalsIgnoreCase(status) || "SUSPENDED".equalsIgnoreCase(status) || "BLOCKED".equalsIgnoreCase(status)) {
            profile.setActive(false);
            if ("BLOCKED".equalsIgnoreCase(status)) {
                profile.setBlocked(true);
            }
        }
        UserProfile saved = profileRepository.save(profile);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public UserProfileDto assignDeliveryZone(Long id, String zone) {
        UserProfile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with id: " + id));
        profile.setDeliveryZone(zone);
        UserProfile saved = profileRepository.save(profile);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileDto> searchCustomers(String query) {
        List<UserProfile> customers = profileRepository.findByRole(Role.CUSTOMER);
        if (query == null || query.isBlank()) {
            return customers.stream().map(this::mapToDto).collect(Collectors.toList());
        }
        String q = query.toLowerCase().trim();
        return customers.stream()
                .filter(c -> (c.getFullName() != null && c.getFullName().toLowerCase().contains(q))
                        || (c.getEmail() != null && c.getEmail().toLowerCase().contains(q))
                        || (c.getMobileNumber() != null && c.getMobileNumber().contains(q)))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private UserProfileDto mapToDto(UserProfile profile) {
        return UserProfileDto.builder()
                .profileId(profile.getProfileId())
                .fullName(profile.getFullName())
                .businessName(profile.getBusinessName())
                .email(profile.getEmail())
                .mobileNumber(profile.getMobileNumber())
                .about(profile.getAbout())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .role(profile.getRole())
                .profileImage(profile.getProfileImage())
                .address(profile.getAddress())
                .active(profile.getActive() != null ? profile.getActive() : true)
                .blocked(profile.getBlocked() != null ? profile.getBlocked() : false)
                .accountStatus(profile.getAccountStatus() != null ? profile.getAccountStatus() : "APPROVED")
                .deliveryZone(profile.getDeliveryZone())
                .rejectionReason(profile.getRejectionReason())
                .vehicleType(profile.getVehicleType())
                .vehicleNumber(profile.getVehicleNumber())
                .vehicleModel(profile.getVehicleModel())
                .drivingLicenseNumber(profile.getDrivingLicenseNumber())
                .drivingLicenseUrl(profile.getDrivingLicenseUrl())
                .vehicleRegistrationUrl(profile.getVehicleRegistrationUrl())
                .identityDocumentUrl(profile.getIdentityDocumentUrl())
                .availability(profile.getAvailability() != null ? profile.getAvailability() : "AVAILABLE")
                .build();
    }
}
