package com.example.gateway.integrations.wemabank.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WemaAccountLookupResponse {
    @JsonProperty("accountname")
    private String accountName;
    private String status;
    @JsonProperty("status_desc")
    private String statusDescription;
}
