package com.example.gateway.commons.utils;

public class PaymentUtils {
    public static String detectCardScheme(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return "Unknown";
        }

        String visaPattern = "^4[0-9]{12}(?:[0-9]{3})?$";
        String mastercardPattern = "^5[1-5][0-9]{14}$|^2(22[1-9]|2[3-9][0-9]|[3-6][0-9]{2}|7[01][0-9]|720)[0-9]{12}$";
        String amexPattern = "^3[47][0-9]{13}$";
        String discoverPattern = "^6(?:011|5[0-9]{2})[0-9]{12}$";
        String vervePattern = "^506$";

        if (cardNumber.matches(visaPattern)) {
            return "VISA";
        } else if (cardNumber.matches(mastercardPattern)) {
            return "MASTERCARD";
        } else if (cardNumber.matches(amexPattern)) {
            return "AMEX";
        } else if (cardNumber.matches(discoverPattern)) {
            return "DISCOVER";
        } else if (cardNumber.matches(vervePattern)) {
            return "VERVE";
        } else {
            return "UNKNOWN";
        }
    }
}
