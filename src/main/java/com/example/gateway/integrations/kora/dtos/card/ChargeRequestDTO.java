package com.example.gateway.integrations.kora.dtos.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChargeRequestDTO {
    @JsonProperty("charge_data")
    private String chargeData;
}
