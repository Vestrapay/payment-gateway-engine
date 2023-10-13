package com.example.gateway.integrations.wemabank.accounts.models;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("wema_accounts")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class WemaAccounts {
    @Id
    @Column("id")
    private Long id;
    @Column("uuid")
    private String uuid;
    @Column("merchant_id")
    private String merchantId;
    @Column("account_name")
    private String accountName;
    @Column("account_number")
    private String accountNumber;
    @CreatedDate
    @Column("created_at")
    private LocalDateTime dateCreated;
    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime dateUpdated;
}
