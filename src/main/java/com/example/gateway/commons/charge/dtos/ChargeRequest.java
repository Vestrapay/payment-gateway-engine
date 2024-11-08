package com.example.gateway.commons.charge.dtos;

import com.example.gateway.commons.charge.enums.ChargeCategory;
import com.example.gateway.commons.charge.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ChargeRequest {
    @NotBlank
    private String merchantId;
    private PaymentMethod paymentMethod;
    private ChargeCategory category;
    private BigDecimal percentage;
    private BigDecimal floor;
    private BigDecimal cap;
    private BigDecimal flatFee;
    private boolean useFlatFee;
}
