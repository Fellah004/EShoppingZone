package com.eshoppingzone.profile.dto;

import com.eshoppingzone.profile.entity.Address;
import com.eshoppingzone.profile.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String businessName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;

    private String about;

    private LocalDate dateOfBirth;

    private String gender;

    @NotNull(message = "Role is required")
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
}
