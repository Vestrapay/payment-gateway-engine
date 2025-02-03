package com.example.gateway.commons.jobs;

import com.example.gateway.commons.notificatioin.NotificationService;
import com.example.gateway.commons.settlements.factory.SettlementFactory;
import com.example.gateway.commons.settlements.models.Settlement;
import com.example.gateway.commons.settlements.repository.SettlementRepository;
import com.example.gateway.commons.transactions.enums.Status;
import com.example.gateway.commons.transactions.reporitory.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
@RequiredArgsConstructor
public class SettlementJob {
    private final TransactionRepository transactionRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementFactory settlementFactory;
    private final NotificationService notificationService;
    @Value("${settlement.provider}")
    private String settlementProvider;
    @Value("${spring.profiles.active}")
    String profile;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Scheduled(cron = "0 0 0 * * *")
    public void doNgnSettlement() {
        log.info("Starting auto NGN settlement");

        if (!profile.equalsIgnoreCase("cron")) {
            log.info("Profile not active for cron jobs");
            return;
        }

        if (!isRunning.compareAndSet(false, true)) {
            log.info("Settlement process is already running");
            return;
        }

        try {
            log.info("Settlement process started");
            LocalDateTime now = LocalDateTime.now();
            transactionRepository.findByTransactionStatusAndSettlementStatusAndCreatedAtBetweenAndCurrency(
                            Status.SUCCESSFUL, Status.PENDING,
                            now.toLocalDate().minusDays(10).atStartOfDay(),
                            now.toLocalDate().atStartOfDay(),
                            "NGN")
                    .collectList()
                    .flatMap(transactions -> {
                        transactions.parallelStream()
                                .forEach(transaction -> {
                                    String merchantId = transaction.getMerchantId();
                                    settlementRepository.findByMerchantIdAndPrimaryAccount(merchantId, true)
                                            .collectList()
                                            .doOnNext(settlements -> {
                                                if (settlements.isEmpty()) {
                                                    transaction.setActivityStatus("No settlement account found for merchant");
                                                    transactionRepository.save(transaction).subscribe();
                                                    throw new RuntimeException("No settlement account found for merchant " + merchantId);
                                                }
                                                Settlement settlementAccount = settlements.stream()
                                                        .filter(Settlement::isPrimaryAccount)
                                                        .findFirst()
                                                        .orElse(settlements.get(0));

                                                // Push transaction to Provider for settlement
                                                settlementFactory.getSettlementService("KORAPAY")
                                                        .pushSettlement(transaction, settlementAccount)
                                                        .doOnNext(transaction1 -> {
                                                            if (transaction.getSettlementStatus().equals(Status.SUCCESSFUL))
                                                                notificationService.postSettlementNotification(transaction1);
                                                            transactionRepository.save(transaction1).subscribe();
                                                        })
                                                        .doOnError(throwable -> log.error(throwable.getMessage()))
                                                        .subscribe();
                                            })
                                            .doOnError(throwable -> log.error("Error is {}", throwable.getMessage()))
                                            .subscribe();
                                });
                        return Mono.empty();
                    }).subscribe();
        } catch (Exception e) {
            log.error("Error during settlement: {}", e.getMessage());
        } finally {
            isRunning.set(false);
            log.info("Settlement process completed");
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void doUSDSettlement() {
        log.info("Starting auto USD settlement");

        if (!profile.equalsIgnoreCase("cron")) {
            log.info("Profile not active for cron jobs");
            return;
        }

        if (!isRunning.compareAndSet(false, true)) {
            log.info("Settlement process is already running");
            return;
        }

        try {
            log.info("Settlement process started");
            LocalDateTime now = LocalDateTime.now();
            transactionRepository.findByTransactionStatusAndSettlementStatusAndCreatedAtBetweenAndCurrency(
                            Status.SUCCESSFUL, Status.PENDING,
                            now.toLocalDate().minusDays(30).atStartOfDay(),
                            now.toLocalDate().minusDays(8).atStartOfDay(),
                            "USD")
                    .collectList()
                    .flatMap(transactions -> {
                        transactions.parallelStream()
                                .forEach(transaction -> {
                                    String merchantId = transaction.getMerchantId();
                                    settlementRepository.findByMerchantIdAndPrimaryAccountAndCurrency(
                                                    merchantId, true, transaction.getCurrency())
                                            .collectList()
                                            .doOnNext(settlements -> {
                                                if (settlements.isEmpty()) {
                                                    transaction.setActivityStatus("No settlement account found for merchant");
                                                    transactionRepository.save(transaction).subscribe();
                                                    throw new RuntimeException("No settlement account found for merchant " + merchantId);
                                                }
                                                Settlement settlementAccount = settlements.stream()
                                                        .filter(Settlement::isPrimaryAccount)
                                                        .findFirst()
                                                        .orElse(settlements.get(0));

                                                // Push transaction to Provider for settlement
                                                settlementFactory.getSettlementService("BUDPAT")
                                                        .pushSettlement(transaction, settlementAccount)
                                                        .doOnNext(transaction1 -> {
                                                            if (transaction.getSettlementStatus().equals(Status.SUCCESSFUL))
                                                                notificationService.postSettlementNotification(transaction1);
                                                            transactionRepository.save(transaction1).subscribe();
                                                        })
                                                        .doOnError(throwable -> log.error(throwable.getMessage()))
                                                        .subscribe();
                                            })
                                            .doOnError(throwable -> log.error("Error is {}", throwable.getMessage()))
                                            .subscribe();
                                });
                        return Mono.empty();
                    }).subscribe();
        } catch (Exception e) {
            log.error("Error during settlement: {}", e.getMessage());
        } finally {
            isRunning.set(false);
            log.info("Settlement process completed");
        }
    }


}
