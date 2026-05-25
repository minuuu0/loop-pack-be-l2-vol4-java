package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductInfo;

import java.math.BigDecimal;

public class ProductV1Dto {

    public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        long likeCount,
        Long brandId,
        boolean available
    ) {
        public static ProductResponse from(ProductInfo info) {
            return new ProductResponse(
                info.id(),
                info.name(),
                info.description(),
                info.price(),
                info.likeCount(),
                info.brandId(),
                info.stock() > 0
            );
        }
    }
}
