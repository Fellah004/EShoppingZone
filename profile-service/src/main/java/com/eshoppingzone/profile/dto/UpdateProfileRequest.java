package com.eshoppingzone.profile.dto;

import com.eshoppingzone.profile.entity.Address;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String businessName;

    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;

    private String about;

    private LocalDate dateOfBirth;

    private String gender;

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
}
