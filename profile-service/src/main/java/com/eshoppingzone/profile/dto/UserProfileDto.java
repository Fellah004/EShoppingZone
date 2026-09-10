package com.eshoppingzone.profile.dto;

import com.eshoppingzone.profile.entity.Address;
import com.eshoppingzone.profile.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDto {

    private Long profileId;
    private String fullName;
    private String businessName;
    private String email;
    private String mobileNumber;
    private String about;
    private LocalDate dateOfBirth;
    private String gender;
    private Role role;
    private String profileImage;
    private Address address;
    private String vehicleType;
    private String vehicleNumber;
    private String vehicleModel;
    private String drivingLicenseNumber;
    private String drivingLicenseUrl;
    private String vehicleRegistrationUrl;
    private String identityDocumentUrl;
    private String availability;
    private Boolean active;
    private Boolean blocked;
    private String accountStatus;
    private String deliveryZone;
    private String rejectionReason;

    public UserProfileDto(Long profileId, String fullName, String email, String mobileNumber, String about, LocalDate dateOfBirth, String gender, Role role, String profileImage, Address address) {
        this.profileId = profileId;
        this.fullName = fullName;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.about = about;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.role = role;
        this.profileImage = profileImage;
        this.address = address;
        this.active = true;
    }
}
