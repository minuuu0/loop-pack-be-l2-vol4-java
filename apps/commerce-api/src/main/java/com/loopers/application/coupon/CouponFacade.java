package com.loopers.application.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.CouponStatus;
import com.loopers.domain.coupon.UserCoupon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class CouponFacade {
    private final CouponService couponService;

    public IssuedCouponInfo issue(Long userId, Long couponId) {
        UserCoupon userCoupon = couponService.issue(userId, couponId);
        return IssuedCouponInfo.from(userCoupon);
    }

    public List<UserCouponInfo> getMyCoupons(Long userId) {
        List<UserCoupon> userCoupons = couponService.getMyCoupons(userId);
        if (userCoupons.isEmpty()) {
            return List.of();
        }

        List<Long> couponIds = userCoupons.stream()
            .map(UserCoupon::getCouponId)
            .distinct()
            .toList();
        Map<Long, Coupon> coupons = couponService.getCoupons(couponIds).stream()
            .collect(Collectors.toMap(Coupon::getId, Function.identity()));

        LocalDateTime now = LocalDateTime.now();
        return userCoupons.stream()
            .map(userCoupon -> {
                Coupon coupon = coupons.get(userCoupon.getCouponId());
                CouponStatus status = userCoupon.resolveStatus(coupon.getExpiredAt(), now);
                return UserCouponInfo.of(userCoupon, coupon, status);
            })
            .toList();
    }
}
