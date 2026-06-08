package com.loopers.domain.coupon;

import com.loopers.domain.money.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Component
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @Transactional
    public Coupon register(String name, Discount discount, Money minOrderAmount, LocalDateTime expiredAt) {
        Coupon coupon = new Coupon(name, discount, minOrderAmount, expiredAt);
        return couponRepository.save(coupon);
    }

    @Transactional
    public UserCoupon issue(Long userId, Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[couponId = " + couponId + "] 쿠폰을 찾을 수 없습니다."));
        UserCoupon userCoupon = new UserCoupon(userId, coupon.getId());
        return userCouponRepository.save(userCoupon);
    }

    @Transactional
    public Coupon update(Long couponId, String name, Discount discount, Money minOrderAmount, LocalDateTime expiredAt) {
        Coupon coupon = getCoupon(couponId);
        coupon.update(name, discount, minOrderAmount, expiredAt);
        return couponRepository.save(coupon);
    }

    @Transactional
    public void delete(Long couponId) {
        Coupon coupon = getCoupon(couponId);
        coupon.delete();
        couponRepository.save(coupon);
    }

    @Transactional(readOnly = true)
    public Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[couponId = " + couponId + "] 쿠폰을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<Coupon> getCouponPage(int page, int size) {
        return couponRepository.findAll(page, size);
    }

    @Transactional(readOnly = true)
    public long countCoupons() {
        return couponRepository.count();
    }

    @Transactional(readOnly = true)
    public List<UserCoupon> getMyCoupons(Long userId) {
        return userCouponRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<UserCoupon> getIssues(Long couponId, int page, int size) {
        return userCouponRepository.findByCouponId(couponId, page, size);
    }

    @Transactional(readOnly = true)
    public long countIssues(Long couponId) {
        return userCouponRepository.countByCouponId(couponId);
    }

    @Transactional(readOnly = true)
    public List<Coupon> getCoupons(List<Long> couponIds) {
        return couponRepository.findAllById(couponIds);
    }
}
