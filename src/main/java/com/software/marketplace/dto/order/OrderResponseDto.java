package com.software.marketplace.dto.order;

import com.software.marketplace.entity.enums.OrderStatus;
import com.software.marketplace.entity.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDto {

    private Long id;
    private Long buyerId;
    private String buyerUsername;
    private Long productId;
    private String productName;
    private String sellerUsername;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private Integer itemCount;
    private Integer quantity;
    private BigDecimal totalAmount;
    private LocalDateTime orderDate;
}
