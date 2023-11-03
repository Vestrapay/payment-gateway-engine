package com.example.gateway.integrations.kora.dtos.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Avs {
    private String state;
    private String city;
    private String country;
    private String address;
    @JsonProperty("zip_code")
    private String zipCode;
}
