package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ProductFacade {
    private final ProductService productService;

    public ProductInfo getProduct(Long id) {
        Product product = productService.getProduct(id);
        return ProductInfo.from(product);
    }
}
