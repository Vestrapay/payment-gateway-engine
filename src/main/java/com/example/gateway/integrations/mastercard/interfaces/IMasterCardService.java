package com.example.gateway.integrations.mastercard.interfaces;

import com.example.gateway.integrations.mastercard.dto.PaymentByCardRequestVO;
import com.example.gateway.integrations.mastercard.dto.PaymentByCardResponseVO;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.mastercard.dto.MasterCardMakePaymentResponse;
import reactor.core.publisher.Mono;

public interface IMasterCardService {
    Mono<Response<MasterCardMakePaymentResponse>> makePayment(PaymentByCardRequestVO request);
    Mono<Response<PaymentByCardResponseVO>> doTsq();

}
