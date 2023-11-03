package com.example.gateway.commons.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("payment_provider")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class Providers {
    @Id
    private Long id;
    private String uuid;
    @Column("provider_name")
    private String providerName;
    @Column("payment_types")
    private String paymentTypes;
}
