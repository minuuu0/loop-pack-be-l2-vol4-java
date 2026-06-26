package com.loopers.interfaces.api.payment;

import com.loopers.domain.payment.PaymentStatus;

public class PaymentV1Dto {

    /** PG 콜백 본문. 우리가 쓰지 않는 필드(cardType/cardNo/amount/orderId 등)는 역직렬화에서 무시된다. */
    public record CallbackRequest(
        String transactionKey,
        PaymentStatus status,
        String reason
    ) {}
}
