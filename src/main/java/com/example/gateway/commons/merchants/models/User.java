package com.example.gateway.commons.merchants.models;

import com.example.gateway.commons.keys.enums.KeyUsage;
import com.example.gateway.commons.merchants.enums.UserType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table("app_user")
@Builder
public class User{
    @Id
    @Column("id")
    private Long id;
    private String uuid;
    private String country;
    @Column("first_name")
    private String firstName;
    @Column("last_name")
    private String lastName;
    private String email;
    @Column("phone_number")
    private String phoneNumber;
    @Column("business_name")
    private String businessName;
    @Column("merchant_id")
    private String merchantId;
    @Column("referral_code")
    private String referralCode;
    @JsonIgnore
    private String password;
    @Enumerated(EnumType.STRING)
    @Column("user_type")
    private UserType userType;
    private KeyUsage environment;
    @Column("created_at")
    @CreatedDate
    private LocalDateTime createdAt;
    @Column("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;
    private boolean enabled;
    @JsonIgnore
    private String otp;
    @Column("kyc_completed")
    private boolean kycCompleted;
    @Column("parent_merchant")
    private boolean parentMerchant;
    @Column("required_documents")
    private String requiredDocuments;

}
