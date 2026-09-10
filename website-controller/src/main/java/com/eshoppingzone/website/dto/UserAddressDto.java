package com.eshoppingzone.website.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddressDto {

    private Long addressId;
    private Long profileId;
    private String fullName;
    private String mobileNumber;
    private String houseNumber;
    private String streetName;
    private String colonyName;
    private String city;
    private String state;
    private String pincode;
    private Boolean isDefault;

    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        if (houseNumber != null && !houseNumber.isBlank()) sb.append(houseNumber).append(", ");
        if (streetName != null && !streetName.isBlank()) sb.append(streetName).append(", ");
        if (colonyName != null && !colonyName.isBlank()) sb.append(colonyName).append(", ");
        if (city != null && !city.isBlank()) sb.append(city).append(", ");
        if (state != null && !state.isBlank()) sb.append(state).append(" - ");
        if (pincode != null && !pincode.isBlank()) sb.append(pincode);
        return sb.toString();
    }
}
