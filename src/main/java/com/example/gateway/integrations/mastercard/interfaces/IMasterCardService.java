package com.example.gateway.integrations.mastercard.interfaces;

import com.example.gateway.commons.cardpayment.PaymentByCardRequestVO;
import com.example.gateway.commons.cardpayment.PaymentByCardResponseVO;
import com.example.gateway.commons.utils.Response;
import reactor.core.publisher.Mono;

public interface IMasterCardService {
    Mono<Response<PaymentByCardResponseVO>> makePayment(PaymentByCardRequestVO request);
    Mono<Response<PaymentByCardResponseVO>> doTsq();

}
