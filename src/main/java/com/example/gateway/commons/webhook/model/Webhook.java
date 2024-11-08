package com.example.gateway.commons.webhook.model;

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
@Builder
@Table("webhook")
public class Webhook {
    @Id
    @Column("id")
    private Long id;
    private String uuid;
    @Column("merchant_id")
    private String merchantId;
    private String url;
    @Column("secret_hash")
    private String secretHash;
    @CreatedDate
    @Column("created_at")
    private LocalDateTime dateCreated;
    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime dateUpdated;
}
