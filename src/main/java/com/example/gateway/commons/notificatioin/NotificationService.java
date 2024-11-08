package com.example.gateway.commons.notificatioin;

import com.example.gateway.commons.notificatioin.repository.NotificationRepository;
import com.example.gateway.commons.transactions.models.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    public void postNotification(Transaction transaction){
        Mono.fromRunnable(() -> notificationRepository.save(buildNotification(transaction)).subscribe()).subscribe();
    }
    public void postSettlementError(String message, String merchantId){
        Mono.fromRunnable(() -> notificationRepository.save(buildSettlementError(message, merchantId)).subscribe()).subscribe();
    }

    public void postSettlementNotification(Transaction transaction){
        Mono.fromRunnable(() -> notificationRepository.save(buildSettlementNotification(transaction)).subscribe()).subscribe();
    }

    public Notification buildNotification(Transaction transaction){
        return Notification.builder()
                .uuid(UUID.randomUUID().toString())
                .merchantId(transaction.getMerchantId())
                .message(transaction.getPaymentType().name()+" transaction performed. " +
                        "Amount: "+transaction.getCurrency()+transaction.getAmount().toString()
                        +" Status: "+transaction.getTransactionStatus().name())
                .build();

    }

    public Notification buildSettlementError(String message,String merchantId){
        return Notification.builder()
                .uuid(UUID.randomUUID().toString())
                .merchantId(merchantId)
                .message(message)
                .build();

    }

    public Notification buildSettlementNotification(Transaction transaction){
        return Notification.builder()
                .uuid(UUID.randomUUID().toString())
                .merchantId(transaction.getMerchantId())
                .message(transaction.getPaymentType().name()+" transaction performed. " +
                        "Amount: "+transaction.getCurrency()+transaction.getAmount().toString()
                        +" Status: "+transaction.getTransactionStatus().name()+" has been settled successfully")
                .build();

    }
}
