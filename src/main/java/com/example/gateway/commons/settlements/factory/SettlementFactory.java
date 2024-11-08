package com.example.gateway.commons.settlements.factory;

import com.example.gateway.commons.settlements.service.ISettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementFactory {
    private final List<ISettlementService> settlementService;
    @Value("${settlement.provider}")
    private String settlementProvider;

    public ISettlementService getSettlementService(){
        return settlementService.stream().filter(iSettlementService -> iSettlementService.getProvider().equalsIgnoreCase(settlementProvider)).findFirst()
                .orElseThrow(() -> {throw new RuntimeException("Settlement provider implementation not found");});

    }
}
