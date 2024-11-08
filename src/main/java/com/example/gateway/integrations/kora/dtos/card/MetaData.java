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
public class MetaData {
    private String internalRef;
    private int age;
    private boolean fixed;
    @JsonProperty("gateway_code")
    private String gatewayCode;
    @JsonProperty("can_resend_otp")
    private boolean canResendOtp;
    @JsonProperty("otp_attempts_left")
    private int otpAttemptsLeft;
    @JsonProperty("support_message")
    private String supportMessage;
}
