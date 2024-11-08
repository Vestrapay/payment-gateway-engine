package com.example.gateway.commons.banks.service;

import com.example.gateway.commons.banks.model.BankList;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ListBanksService {
    private static final String BANK_FILE  = "banks.json";
    private static BankList bankList;
    @PostConstruct
    public void init() {
        try {
            loadBanks();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load banks", e);
        }
    }


    private static void loadBanks() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ClassPathResource resource = new ClassPathResource(BANK_FILE);
        bankList= objectMapper.readValue(resource.getInputStream(), new TypeReference<BankList>() {});
    }

    public static BankList getBanks(){
        return bankList;
    }
}
