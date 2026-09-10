package com.eshoppingzone.profile.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    private String houseNumber;
    private String streetName;
    private String colonyName;
    private String city;
    private String state;
    private String pincode;
}
