package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Product V1 API", description = "Loopers 상품 API 입니다.")
public interface ProductV1ApiSpec {

    @Operation(
        summary = "상품 조회",
        description = "상품 ID 로 상품 정보를 조회합니다."
    )
    ApiResponse<ProductV1Dto.ProductResponse> getProduct(
        @Parameter(name = "productId", in = ParameterIn.PATH, required = true, description = "상품 ID")
        Long productId
    );
}
