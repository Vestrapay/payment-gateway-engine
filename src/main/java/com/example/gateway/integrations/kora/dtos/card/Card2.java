package com.example.gateway.integrations.kora.dtos.card;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Card2 {
    private String name;
    private String number;
    private String cvv;
    @JsonProperty("expiry_month")
    private String expiryMonth;
    @JsonProperty("expiry_year")
    private String expiryYear;
    private String pin;

}
