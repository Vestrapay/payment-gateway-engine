package com.example.gateway.commons.jobs;

import com.example.gateway.integrations.wemabank.interfaces.IWemaBankService;
import com.example.gateway.integrations.wemabank.services.WemaBankService;
import com.example.gateway.transactions.enums.Status;
import com.example.gateway.transactions.reporitory.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class SettlementJob {
    private final TransactionRepository transactionRepository;
    private final IWemaBankService wemaBankService;
    @Profile("cron")
    @Scheduled(fixedDelay = 15000)
    public void doSettlement(){

    }
}
