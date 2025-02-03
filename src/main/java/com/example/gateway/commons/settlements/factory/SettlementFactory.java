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


    public ISettlementService getSettlementService(String provider){
        return settlementService.stream().filter(iSettlementService -> iSettlementService.getProvider().equalsIgnoreCase(provider)).findFirst()
                .orElseThrow(() -> {throw new RuntimeException("Settlement provider implementation not found");});

    }
}
