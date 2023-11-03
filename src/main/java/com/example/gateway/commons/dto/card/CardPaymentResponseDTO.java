package com.example.gateway.commons.dto.card;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardPaymentResponseDTO {
    private boolean status;
    private Object responseObject;


}
