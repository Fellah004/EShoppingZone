package com.eshoppingzone.profile.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long profileId;

    @Column(nullable = false)
    private String fullName;

    private String businessName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String mobileNumber;

    private String about;

    private LocalDate dateOfBirth;

    private String gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String profileImage;

    @Embedded
    private Address address;

    private String vehicleType;
    private String vehicleNumber;
    private String vehicleModel;
    private String drivingLicenseNumber;

    private String drivingLicenseUrl;
    private String vehicleRegistrationUrl;
    private String identityDocumentUrl;

    @Column(nullable = false)
    @Builder.Default
    private String availability = "AVAILABLE";

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean blocked = false;

    @Column(nullable = false)
    @Builder.Default
    private String accountStatus = "APPROVED";

    private String deliveryZone;

    private String rejectionReason;

    public UserProfile(Long profileId, String fullName, String email, String password, String mobileNumber, String about, LocalDate dateOfBirth, String gender, Role role, String profileImage, Address address) {
        this.profileId = profileId;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
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
