package com.loopers.application.like;

import com.loopers.config.CacheConfig;
import com.loopers.domain.like.ProductLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class LikeFacade {
    private final ProductLikeService productLikeService;

    // 좋아요가 바뀌면 상세 응답의 likeCount 가 낡으므로, 해당 상품의 상세 캐시를 무효화한다.
    @CacheEvict(cacheNames = CacheConfig.PRODUCT_DETAIL, key = "#productId")
    @Transactional
    public void like(Long userId, Long productId) {
        productLikeService.like(userId, productId);
    }

    @CacheEvict(cacheNames = CacheConfig.PRODUCT_DETAIL, key = "#productId")
    @Transactional
    public void unlike(Long userId, Long productId) {
        productLikeService.unlike(userId, productId);
    }
}
