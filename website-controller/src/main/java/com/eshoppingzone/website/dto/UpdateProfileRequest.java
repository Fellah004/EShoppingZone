package com.eshoppingzone.website.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    private String fullName;
    private String mobileNumber;
    private String about;
    private String dateOfBirth;
    private String gender;
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

    public UpdateProfileRequest(String fullName, String mobileNumber, String dateOfBirth, String gender, String profileImage, AddressDto address) {
        this.fullName = fullName;
        this.mobileNumber = mobileNumber;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.profileImage = profileImage;
        this.address = address != null ? address : new AddressDto();
    }
}
