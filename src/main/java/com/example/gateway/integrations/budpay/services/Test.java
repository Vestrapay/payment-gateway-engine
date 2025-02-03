//package com.example.gateway.integrations.budpay.services;
//
//import com.example.gateway.api.card.dtos.Card;
//import com.example.gateway.api.card.dtos.CardPaymentRequestDTO;
//import com.example.gateway.commons.utils.HttpUtil;
//import com.example.gateway.commons.utils.Response;
//import com.example.gateway.integrations.kora.dtos.card.Customer;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class Test implements CommandLineRunner {
//    private final BudPayService budPayService;
//    private final HttpUtil httpUtil;
//    @Override
//    public void run(String... args) throws Exception {
//        Response<?> block = budPayService.payWithCard(CardPaymentRequestDTO.builder()
//                        .amount(BigDecimal.ONE)
//                        .card(Card.builder()
//                                .cvv("647")
//                                .expiryMonth("10")
//                                .expiryYear("28")
//                                .pin("1234")
//                                .number("4596610180241216")
//                                .name("OLAWALE BAKARE")
//                                .build())
//                        .customerDetails(Customer.builder().email("Ifebakare@vestrapay.com").name("OLAWALE BAKARE")
//                                .build())
//                        .currency("USD")
//                        .transactionReference("12345")
//                .build(), "12345", "12345").block();
//        log.info("{}",block);
//    }
//}
