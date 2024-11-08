package com.example.gateway.commons.settlements.models;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table("settlement")
@Builder
public class Settlement {
    @Id
    @Column("id")
    private Long id;
    private String uuid;
    private String country;
    @Column("merchant_id")
    private String merchantId;
    private String currency;
    @Column("bank_name")
    private String bankName;
    @Column("account_number")
    private String accountNumber;
    @Column("primary_account")
    private boolean primaryAccount;
    @Column("created_at")
    @CreatedDate
    private LocalDateTime createdAt;
    @Column("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

}
