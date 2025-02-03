package com.example.gateway.integrations.kora.dtos.card;

import jakarta.validation.constraints.Email;
import lombok.*;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Customer {
    private String name;
    @Email(message = "email must be valid")
    private String email;
}
