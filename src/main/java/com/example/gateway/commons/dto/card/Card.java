package com.example.gateway.commons.dto.card;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Card {
    private String name;
    private String number;
    private String cvv;
    private String pin;
    private String expiryMonth;
    private String expiryYear;

}
