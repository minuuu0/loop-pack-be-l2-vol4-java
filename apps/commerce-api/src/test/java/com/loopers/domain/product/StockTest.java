package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StockTest {
    @DisplayName("재고를 생성할 때, ")
    @Nested
    class Create {
        @DisplayName("0 이상의 수량이면, 정상적으로 생성된다.")
        @Test
        void createsStock_whenQuantityIsZeroOrPositive() {
            // arrange
            int quantity = 10;

            // act
            Stock stock = new Stock(quantity);

            // assert
            assertThat(stock.getQuantity()).isEqualTo(quantity);
        }

        @DisplayName("음수 수량이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenQuantityIsNegative() {
            // arrange
            int quantity = -1;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new Stock(quantity);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
