package com.example.gateway.integrations.mastercard.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

public enum Currency {

    NGN("NGN", "566"),
    GHS("GHS", "936"),
    USD("USD", "840");


    Currency(String code, String iso4217) {
        this.code = code;
        this.iso4217 = iso4217;
    }

    private final String code;
    private final String iso4217;

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getIsoCode() {
        return iso4217;
    }

    public static boolean isNigerianCurrency(Currency currency) {
        return StringUtils.equalsIgnoreCase(NGN.code, currency.code);
    }
}
