package com.example.gateway.transactions.enums;

public enum PaymentTypeEnum {
    CARD("CARD"),
    PAYMENT_LINK("PAYMENT LINK"),
    TRANSFER("TRANSFER"),
    NQR("NQR");

    private final String description;

    PaymentTypeEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
