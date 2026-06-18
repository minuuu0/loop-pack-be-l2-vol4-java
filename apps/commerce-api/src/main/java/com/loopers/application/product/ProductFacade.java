package com.loopers.application.product;

import com.loopers.application.support.PageResult;
import com.loopers.config.CacheConfig;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductSort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class ProductFacade {
    private final ProductService productService;
    private final BrandService brandService;

    // 상세는 키가 상품 id 와 1:1 이라 캐시 적중률이 높고, 좋아요 변경 시 그 키 하나만 무효화하면 된다.
    @Cacheable(cacheNames = CacheConfig.PRODUCT_DETAIL, key = "#id")
    public ProductInfo getProduct(Long id) {
        Product product = productService.getProduct(id);
        Brand brand = brandService.getBrand(product.getBrandId());
        return ProductInfo.from(product, brand);
    }

    // 목록은 brandId·정렬·페이지 조합마다 키가 달라진다. 응답을 결정하는 모든 입력을 키에 담아야 다른 조합끼리 섞이지 않는다.
    @Cacheable(
        cacheNames = CacheConfig.PRODUCT_LIST,
        key = "(#brandId == null ? 'all' : #brandId) + ':' + #sortValue + ':' + #page + ':' + #size"
    )
    public PageResult<ProductInfo> getProducts(Long brandId, String sortValue, int page, int size) {
        ProductSort sort = ProductSort.from(sortValue);
        List<ProductInfo> items = productService.getProducts(brandId, sort, page, size).stream()
            .map(product -> {
                Brand brand = brandService.getBrand(product.getBrandId());
                return ProductInfo.from(product, brand);
            })
            .toList();
        long totalElements = productService.countProducts(brandId);
        return new PageResult<>(items, page, size, totalElements);
    }
}
