package com.example.gateway.integrations.wemabank.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class WemaTransactionNotificationResponse {
    @JsonProperty("transactionreference")
    private String transactionReference;
    private String status;
    @JsonProperty("status_desc")
    private String statusDescription;
}
