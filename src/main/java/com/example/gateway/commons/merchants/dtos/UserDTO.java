package com.example.gateway.commons.merchants.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class UserDTO {
    @NotBlank(message = "country must be provided")
    private String country;
    @NotBlank(message = "firstname must be provided")
    private String firstName;
    @NotBlank(message = "lastname must be provided")
    private String lastName;
    @NotBlank(message = "email must be provided")
    @Email(message = "requires a valid email address")
    private String email;
    @NotBlank(message = "business name must be provided")
    private String businessName;
    private String referralCode;
    private String phoneNumber;
    @NotBlank(message = "password must be provided")
    private String password;
}
