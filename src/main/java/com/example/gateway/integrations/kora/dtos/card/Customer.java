package com.example.gateway.integrations.kora.dtos.card;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Customer {
    private String name;
    @Email(message = "email must be valid")
    private String email;
}
