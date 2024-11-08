package com.example.gateway.commons.settlements.models;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table("settlement_duration")
@Builder
public class SettlementDurations {
    private Long id;
    private String uuid;
    private String duration;
    @CreatedDate
    @Column("date_created")
    private LocalDateTime dateCreated;
    @LastModifiedDate
    @Column("date_updated")
    private LocalDateTime dateUpdated;
}
