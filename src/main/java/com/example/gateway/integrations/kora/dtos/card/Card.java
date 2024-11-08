package com.example.gateway.integrations.kora.dtos.card;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Card {
    private String name;
    private String number;
    private String cvv;
    @JsonProperty("expiry_month")
    private String expiryMonth;
    @JsonProperty("expiry_year")
    private String expiryYear;
    private String pin;
    @JsonProperty("card_type")
    private String cardType;
    @JsonProperty("first_six")
    private String firstSix;
    @JsonProperty("last_four")
    private String lastFour;
    private String expiry;
}
