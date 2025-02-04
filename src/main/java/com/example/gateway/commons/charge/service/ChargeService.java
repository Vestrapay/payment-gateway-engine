package com.example.gateway.commons.charge.service;

import com.example.gateway.commons.charge.enums.ChargeCategory;
import com.example.gateway.commons.charge.enums.PaymentMethod;
import com.example.gateway.commons.charge.model.Charge;
import com.example.gateway.commons.charge.repository.ChargeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Objects;

@RequiredArgsConstructor
@Slf4j
@Service
public class ChargeService {
    private final ChargeRepository chargeRepository;
    @Value("${default.payment.fee}")
    private BigDecimal defaultFee;
    public Mono<BigDecimal> getPaymentCharge(String merchantId, PaymentMethod paymentMethod, ChargeCategory category,BigDecimal amount,String currency){
        return chargeRepository.findByMerchantIdAndPaymentMethodAndCategoryAndCurrency(merchantId,paymentMethod,category,currency)
                .flatMap(charge -> {
                    return Mono.just(computeFee(charge, amount));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    Charge.ChargeBuilder chargeBuilder = Charge.builder()
                            .useFlatFee(true)
                            .flatFee(defaultFee);
                    if (Objects.equals(currency, "USD")){
                        chargeBuilder.flatFee(BigDecimal.valueOf(5));
                    }
                    else {
                        chargeBuilder.flatFee(defaultFee);

                    }

                    return Mono.just(computeFee(chargeBuilder.build(), amount));
                }));

    }

    public BigDecimal computeFee(Charge charge, BigDecimal amount){
        if (charge.isUseFlatFee()){
            BigDecimal flatFee = Objects.requireNonNullElse(charge.getFlatFee(), BigDecimal.ZERO);
            if (flatFee.compareTo(amount)>0){
                return defaultFee;
            }
            return flatFee;
        }
        BigDecimal percentage = charge.getPercentage();
        BigDecimal fee = percentage.multiply(amount);
        BigDecimal floor = Objects.requireNonNullElse(charge.getFloor(), BigDecimal.ZERO);
        BigDecimal cap = Objects.requireNonNullElse(charge.getCap(),BigDecimal.ZERO);

        if (cap.equals(BigDecimal.ZERO)){
            if (fee.compareTo(floor)<=0){
                return floor;
            }
            return fee;
        }
        else {
            if (fee.compareTo(cap)>=0){
                return cap;
            }
            return fee;
        }


    }



}
