package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @Transactional
    public UserCoupon issue(Long userId, Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[couponId = " + couponId + "] 쿠폰을 찾을 수 없습니다."));
        UserCoupon userCoupon = new UserCoupon(userId, coupon.getId());
        return userCouponRepository.save(userCoupon);
    }

    @Transactional(readOnly = true)
    public List<UserCoupon> getMyCoupons(Long userId) {
        return userCouponRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Coupon> getCoupons(List<Long> couponIds) {
        return couponRepository.findAllById(couponIds);
    }
}
