package com.example.gateway.commons.keys.models;

import com.example.gateway.commons.keys.enums.KeyUsage;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table("user_keys")
@Builder
public class Keys {
    @Id
    @Column("id")
    private Long id;
    private String uuid;
    @Column("user_id") //merchant id of the user
    private String userId;
    @Column("key_usage")
    @Enumerated(EnumType.STRING)
    private KeyUsage keyUsage;
    @Column("public_key")
    private String publicKey;
    @Column("secret_key")
    private String secretKey;
    @Column("encryption_key")
    private String encryptionKey;
    @Column("created_at")
    @CreatedDate
    private LocalDateTime createdAt;
    @Column("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
