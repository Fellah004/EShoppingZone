package com.eshoppingzone.website.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    private String fullName;
    private String email;
    private String password;
    private String mobileNumber;
    private String about;
    private String dateOfBirth;
    private String gender;
    private String role;
    private String profileImage;
    private String businessName;
    @Builder.Default
    private AddressDto address = new AddressDto();
    private String vehicleType;
    private String vehicleNumber;
    private String vehicleModel;
    private String drivingLicenseNumber;
    private String drivingLicenseUrl;
    private String vehicleRegistrationUrl;
    private String identityDocumentUrl;
    private String availability;

    public RegisterRequest(String fullName, String email, String password, String mobileNumber, String dateOfBirth, String gender, String role, String profileImage, AddressDto address) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.mobileNumber = mobileNumber;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.role = role;
        this.profileImage = profileImage;
        this.address = address != null ? address : new AddressDto();
    }
}
