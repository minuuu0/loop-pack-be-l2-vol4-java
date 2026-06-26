package com.loopers.application.payment;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.product.ProductService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * PG가 콜백/폴링으로 전해온 최종 결제 상태를 우리 시스템에 반영한다.
 * 콜백과 폴링(Phase 3)이 같은 결제를 동시에 건드릴 수 있으므로, 이미 확정된 결제는 멱등하게 무시한다.
 * 실패면 주문을 FAILED 로 전이하고 재고·쿠폰을 보상(원복)한다 — 한 트랜잭션으로 묶는다.
 */
@RequiredArgsConstructor
@Component
public class PaymentFacade {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final CouponService couponService;

    @Transactional
    public void confirm(String transactionKey, PaymentStatus status, String reason) {
        Payment payment = paymentRepository.findByTransactionKey(transactionKey)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[transactionKey = " + transactionKey + "] 결제를 찾을 수 없습니다."));
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return; // 이미 확정됨 — 콜백 중복/폴링 동시 도착에 대한 멱등 no-op
        }
        Order order = orderRepository.findById(payment.getOrderId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[orderId = " + payment.getOrderId() + "] 주문을 찾을 수 없습니다."));

        switch (status) {
            case SUCCESS -> {
                payment.markSuccess();
                order.markPaid();
            }
            case FAILED -> {
                payment.markFailed(reason);
                order.markFailed();
                compensate(order);
            }
            case PENDING -> {
                // 아직 미확정 통지 — 확정할 것이 없으므로 그대로 둔다
            }
        }
        // 전이는 영속 상태의 엔티티를 수정한 것이라 트랜잭션 커밋 시 dirty checking 으로 반영된다 (별도 save 불필요).
    }

    /** 결제 실패 보상: 차감했던 재고와 사용했던 쿠폰을 되돌린다. */
    private void compensate(Order order) {
        orderRepository.findItemsByOrderId(order.getId())
            .forEach(item -> productService.restoreStock(item.getProductId(), item.getQuantity().getValue()));
        if (order.getCouponId() != null) {
            couponService.restore(order.getUserId(), order.getCouponId());
        }
    }
}
