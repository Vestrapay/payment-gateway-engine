package com.example.gateway.integrations.budpay.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MetaData {
    private String senderName;
    private String senderAddress;

}
