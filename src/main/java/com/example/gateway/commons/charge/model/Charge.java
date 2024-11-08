package com.example.gateway.commons.charge.model;

import com.example.gateway.commons.charge.enums.ChargeCategory;
import com.example.gateway.commons.charge.enums.PaymentMethod;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table("charges")
@Builder

public class Charge {
    @Column("id")
    @Id
    private Long id;
    private String uuid;
    @Column("merchant_id")
    private String merchantId;
    @Enumerated(EnumType.STRING)
    @Column("payment_method")
    private PaymentMethod paymentMethod;
    private BigDecimal percentage;
    private BigDecimal floor;
    private BigDecimal cap;
    @Column("flat_fee")
    private BigDecimal flatFee;
    @Column("use_flat_fee")
    private boolean useFlatFee;
    @Enumerated(EnumType.STRING)
    private ChargeCategory category;
    @Column("created_at")
    @CreatedDate
    private LocalDateTime createdAt;
    @Column("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
