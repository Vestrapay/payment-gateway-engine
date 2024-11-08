package com.example.gateway.integrations.kora.dtos.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Data
public class Authorization {
    @JsonProperty("required_fields")
    private ArrayList<String>requiredFields;
}
