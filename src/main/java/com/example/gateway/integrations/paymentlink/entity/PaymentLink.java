package com.example.gateway.integrations.paymentlink.entity;

import com.example.gateway.transactions.enums.Status;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Table("payment_link")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PaymentLink {
    @Id
    private Long id;
    private String uuid;
    private String link;
    private String path;
    @Column("transaction_id")
    private String transactionId;
    @Column("merchant_id")
    private String merchantId;
    @Enumerated(EnumType.STRING)
    private Status status;
    private BigDecimal amount;
    @Column("invoice_id")
    private String invoiceId;
    @Column("expiry_date")
    private LocalDateTime expiryDate;
    @Column("customer_name")
    private String customerName;
    @Column("customer_email")
    private String customerEmail;
    private String params;
    private String description;
    @CreatedDate
    @Column("date_created")
    private LocalDateTime dateCreated;
    @LastModifiedDate
    @Column("date_updated")
    private LocalDateTime dateUpdated;

}

