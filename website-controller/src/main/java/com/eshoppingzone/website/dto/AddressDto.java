package com.eshoppingzone.website.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDto {

    private String fullName;
    private String mobileNumber;
    private String houseNumber;
    private String streetName;
    private String colonyName;
    private String city;
    private String state;
    private String pincode;

    public AddressDto(String houseNumber, String streetName, String colonyName, String city, String state, String pincode) {
        this.houseNumber = houseNumber;
        this.streetName = streetName;
        this.colonyName = colonyName;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
    }

    public String getStreetAddress() {
        StringBuilder sb = new StringBuilder();
        if (houseNumber != null && !houseNumber.isBlank()) sb.append(houseNumber);
        if (streetName != null && !streetName.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(streetName);
        }
        if (colonyName != null && !colonyName.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(colonyName);
        }
        return sb.length() > 0 ? sb.toString() : "";
    }

    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        if (fullName != null && !fullName.isBlank()) sb.append(fullName).append(", ");
        if (houseNumber != null && !houseNumber.isBlank()) sb.append(houseNumber).append(", ");
        if (streetName != null && !streetName.isBlank()) sb.append(streetName).append(", ");
        if (colonyName != null && !colonyName.isBlank()) sb.append(colonyName).append(", ");
        if (city != null && !city.isBlank()) sb.append(city).append(", ");
        if (state != null && !state.isBlank()) sb.append(state).append(" - ");
        if (pincode != null && !pincode.isBlank()) sb.append(pincode);
        if (mobileNumber != null && !mobileNumber.isBlank()) sb.append(" (Phone: ").append(mobileNumber).append(")");
        return sb.toString();
    }
}
