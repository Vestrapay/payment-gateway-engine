package com.example.gateway.commons.transactions.models;

import com.example.gateway.commons.transactions.enums.PaymentTypeEnum;
import com.example.gateway.commons.transactions.enums.Status;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table("vestrapay_transactions")
@Builder
public class Transaction {
    @Column("id")
    @Id
    private Long id;
    private String uuid;
    @Column("payment_type")
    @Enumerated(EnumType.STRING)
    private PaymentTypeEnum paymentType;
    private BigDecimal amount;
    private BigDecimal fee;
    private String pan;
    @Column("transaction_reference")
    private String transactionReference;
    @Column("vestrapay_reference")
    private String vestraPayReference;
    @Column("provider_reference")
    private String providerReference;
    @Column("scheme")
    private String cardScheme;
    @Column("description")
    private String narration;
    @Column("activity_status")
    private String activityStatus;
    @Column("transaction_status")
    private Status transactionStatus;
    @Column("user_id")
    private String userId;
    @Column("merchant_id")
    private String merchantId;
    @Column("provider_name")
    private String providerName;
    @Column("settlement_status")
    private Status settlementStatus;
    @Column("customer_id")
    private String customerId;
    private String currency;
    @Column("meta_data")
    private String metaData;
    @Column("settlement_ref")
    private String settlementReference;
    @Column("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

}
