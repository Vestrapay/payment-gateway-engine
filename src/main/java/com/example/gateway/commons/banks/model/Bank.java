package com.example.gateway.commons.banks.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Bank {
    private String name;
    private String slug;
    private String code;
    @JsonProperty("nibss_bank_code")
    private String nibssBankCode;
    private String country;
}
