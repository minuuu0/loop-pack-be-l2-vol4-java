package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저에게 발급된 쿠폰 한 장. 어떤 템플릿(couponId)을 누가(userId) 가졌고 상태가 무엇인지를 가진다.
 * 발급 시점에는 AVAILABLE 로 시작한다.
 * userId/couponId 는 인증·검증된 흐름에서 오는 내부 보장값이므로 @Column(nullable=false) 에 맡긴다.
 */
@Entity
@Table(name = "user_coupon")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CouponStatus status;

    public UserCoupon(Long userId, Long couponId) {
        this.userId = userId;
        this.couponId = couponId;
        this.status = CouponStatus.AVAILABLE;
    }
}
