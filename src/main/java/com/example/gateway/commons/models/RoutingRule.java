package com.example.gateway.commons.models;

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
@Table("routing_rule")
@Builder
public class RoutingRule {
    @Id
    @Column("id")
    private Long id;
    private String uuid;
    @Column("payment_method")
    private String paymentMethod;
    @Column("provider")
    private String provider;
    @Column("created_at")
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;


}
