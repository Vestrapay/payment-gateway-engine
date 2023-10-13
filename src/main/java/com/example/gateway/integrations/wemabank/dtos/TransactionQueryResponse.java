package com.example.gateway.integrations.wemabank.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionQueryResponse {
    private String status;
    @JsonProperty("status_desc")
    private String statusDescription;
    private List<WemaTransactionDTO> transactions;
}
