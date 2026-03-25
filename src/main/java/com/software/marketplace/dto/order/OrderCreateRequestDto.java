package com.software.marketplace.dto.order;

import com.software.marketplace.entity.enums.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateRequestDto {

    @NotNull
    private Long productId;

    @Min(1)
    private Integer quantity;

    private PaymentMethod paymentMethod;
}
