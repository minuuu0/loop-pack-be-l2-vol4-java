package com.loopers.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Redis 기반 캐시 설정.
 * <p>
 * {@code @EnableCaching} 으로 {@code @Cacheable}/{@code @CacheEvict} 어노테이션을 활성화하고,
 * 캐시 이름별로 서로 다른 TTL(만료 시간)을 가진 {@link RedisCacheManager} 를 등록한다.
 * <ul>
 *   <li>상세({@value #PRODUCT_DETAIL}) — 키가 상품과 1:1이라 좋아요 변경 시 정밀 무효화가 가능 → TTL 길게(10분)</li>
 *   <li>목록({@value #PRODUCT_LIST}) — brandId×sort×page 조합으로 키가 폭발해 정밀 무효화가 불가 → 짧은 TTL(30초)로 알아서 만료</li>
 * </ul>
 */
@EnableCaching
@Configuration
public class CacheConfig implements CachingConfigurer {

    public static final String PRODUCT_DETAIL = "productDetail";
    public static final String PRODUCT_LIST = "productList";

    /**
     * Redis 장애 시 우회(fallback) 처리.
     * <p>
     * 기본 동작은 캐시 조회/저장 중 발생한 예외를 그대로 호출자에게 전파한다. 그러면 Redis 가 죽는 순간
     * 캐시를 쓰는 모든 API 가 함께 죽는다. 캐시는 어디까지나 보조 계층이므로, 에러를 로그만 남기고 삼켜서
     * 캐시 계층을 건너뛰고 원본(DB)으로 그대로 처리되게 한다.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }

    @Bean
    public RedisCacheManager productCacheManager(RedisConnectionFactory connectionFactory) {
        // 키는 사람이 읽을 수 있는 문자열로, 값은 객체를 JSON 으로 직렬화해 저장한다.
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> caches = Map.of(
            PRODUCT_DETAIL, base.entryTtl(Duration.ofMinutes(10)),
            PRODUCT_LIST, base.entryTtl(Duration.ofSeconds(30))
        );

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(base)
            .withInitialCacheConfigurations(caches)
            .build();
    }

    /** 캐시 계층 에러를 로그만 남기고 무시해, DB 로 우회하게 한다. */
    private static class LoggingCacheErrorHandler implements CacheErrorHandler {
        private static final Logger log = LoggerFactory.getLogger(LoggingCacheErrorHandler.class);

        @Override
        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
            log.warn("캐시 조회 실패, DB 로 우회한다. cache={}, key={}", cache.getName(), key, exception);
        }

        @Override
        public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
            log.warn("캐시 저장 실패. cache={}, key={}", cache.getName(), key, exception);
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
            log.warn("캐시 무효화 실패. cache={}, key={}", cache.getName(), key, exception);
        }

        @Override
        public void handleCacheClearError(RuntimeException exception, Cache cache) {
            log.warn("캐시 전체 삭제 실패. cache={}", cache.getName(), exception);
        }
    }
}
