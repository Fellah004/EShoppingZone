package com.eshoppingzone.website.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDto {

    private Long profileId;
    private String fullName;
    private String email;
    private String mobileNumber;
    private String about;
    private String dateOfBirth;
    private String gender;
    private String role;
    private String profileImage;
    private String businessName;
    private AddressDto address;
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

    public UserProfileDto(Long profileId, String fullName, String email, String mobileNumber, String dateOfBirth, String gender, String role, String profileImage, AddressDto address) {
        this.profileId = profileId;
        this.fullName = fullName;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.role = role;
        this.profileImage = profileImage;
        this.address = address;
        this.active = true;
    }

    public UserProfileDto(Long profileId, String fullName, String email, String mobileNumber, String about, String dateOfBirth, String gender, String role, String profileImage, AddressDto address) {
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
