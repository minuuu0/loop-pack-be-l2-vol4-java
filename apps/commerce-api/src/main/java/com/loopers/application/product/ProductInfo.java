package com.loopers.application.product;

import com.loopers.domain.product.Product;

import java.math.BigDecimal;

public record ProductInfo(
    Long id,
    String name,
    String description,
    BigDecimal price,
    int stock,
    long likeCount,
    Long brandId
) {
    public static ProductInfo from(Product product) {
        return new ProductInfo(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice().getAmount(),
            product.getStock().getQuantity(),
            product.getLikeCount(),
            product.getBrandId()
        );
    }
}
