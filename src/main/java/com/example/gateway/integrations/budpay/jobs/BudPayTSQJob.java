package com.example.gateway.integrations.budpay.jobs;

import com.example.gateway.commons.notificatioin.NotificationService;
import com.example.gateway.commons.transactions.enums.Status;
import com.example.gateway.commons.transactions.reporitory.TransactionRepository;
import com.example.gateway.commons.utils.HttpUtil;
import com.example.gateway.integrations.budpay.dtos.TransactionDetailsResponse;
import com.example.gateway.integrations.budpay.services.BudPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.example.gateway.commons.transactions.enums.Status.*;

@Service
@Profile("cron")
@Slf4j
@RequiredArgsConstructor
public class BudPayTSQJob {
    private final TransactionRepository transactionRepository;
    private final BudPayService budPayService;
    private final HttpUtil httpUtil;

    @Value("${budpay.baseurl}")
    private String budPayBaseUrl;
    @Value("${budpay.apiKey}")
    private String authorization;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final NotificationService notificationService;

    @Scheduled(fixedDelay = 5000)
    public void checkStatus() {
        if (isRunning.compareAndSet(false, true)) {
            try {
                transactionRepository.findByProviderNameAndTransactionStatusOrTransactionStatus(budPayService.getName(), Status.PROCESSING, ONGOING)
                        .collectList()
                        .flatMap(transactions -> {
                            log.info("BUDPAY PENDING transaction size is {}",transactions.size());
                            transactions.parallelStream()
                                    .forEach(transaction -> {
                                        httpUtil.get(budPayBaseUrl + "transaction/" + transaction.getTransactionReference() + "/verify", Map.of("authorization","Bearer "+authorization,"content-type","application/json"), 80)
                                                .flatMap(clientResponse2 -> {
                                                    if (clientResponse2.statusCode().is2xxSuccessful()) {
                                                        return clientResponse2.bodyToMono(TransactionDetailsResponse.class)
                                                                .flatMap(transactionDetailsResponse -> {
                                                                    if (transactionDetailsResponse.isStatus() &&
                                                                            Objects.equals(transactionDetailsResponse.getData().getGatewayResponseCode(), "00")) {
                                                                        transaction.setTransactionStatus(SUCCESSFUL);
                                                                    } else if (transactionDetailsResponse.getData().getStatus().equalsIgnoreCase("failed")|| transactionDetailsResponse.getData().getStatus().equalsIgnoreCase("cancelled")) {
                                                                        transaction.setTransactionStatus(FAILED);
                                                                    } else {
                                                                        transaction.setTransactionStatus(PROCESSING);
                                                                    }
                                                                    transactionRepository.save(transaction).subscribe();
                                                                    notificationService.postNotification(transaction);
                                                                    return Mono.empty();
                                                                });
                                                    }
                                                    else
                                                        return clientResponse2.bodyToMono(Map.class)
                                                                .flatMap(map ->{
                                                                    transaction.setTransactionStatus(FAILED);
                                                                    transactionRepository.save(transaction).subscribe();
                                                                    notificationService.postNotification(transaction);
                                                                    return Mono.empty();
                                                                } );
                                                }).subscribe();
                                    });
                            return Mono.empty();
                        }).subscribe();
            } finally {
                isRunning.set(false); // Release the lock
            }
        } else {
            log.info("checkStatus is already running, skipping this cycle.");
        }
    }
}
