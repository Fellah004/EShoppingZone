package com.eshoppingzone.profile.dto;

import com.eshoppingzone.profile.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private String tokenType;
    private Long profileId;
    private String fullName;
    private String email;
    private Role role;
    private String mobileNumber;
}
