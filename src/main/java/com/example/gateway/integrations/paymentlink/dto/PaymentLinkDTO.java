package com.example.gateway.integrations.paymentlink.dto;

import com.example.gateway.integrations.kora.dtos.card.Customer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PaymentLinkDTO {
    @NotNull(message = "amount must be present")
    private BigDecimal amount;
    @NotBlank(message = "invoice must be present")
    private String invoiceId;
    private String description;
    private String customizedLink;
    private int daysActive;
    @NotNull(message = "customer information must be present")
    private Customer customer;
    private Map<String,String>additionalData;
}
