package com.example.gateway.api.card.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Card {
    private String name;
    private String number;
    private String cvv;
    private String pin;
    private String expiryMonth;
    private String expiryYear;

}
