package com.software.marketplace.service.impl;

import com.software.marketplace.dto.order.OrderCreateRequestDto;
import com.software.marketplace.dto.order.OrderResponseDto;
import com.software.marketplace.entity.Order;
import com.software.marketplace.entity.OrderItem;
import com.software.marketplace.entity.Product;
import com.software.marketplace.entity.User;
import com.software.marketplace.entity.enums.OrderStatus;
import com.software.marketplace.entity.enums.RoleType;
import com.software.marketplace.repository.OrderItemRepository;
import com.software.marketplace.repository.OrderRepository;
import com.software.marketplace.repository.ProductRepository;
import com.software.marketplace.repository.UserRepository;
import com.software.marketplace.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public OrderResponseDto placeOrderForBuyer(Long buyerId, OrderCreateRequestDto request) {
        User buyer = findUserWithRole(buyerId, RoleType.ROLE_BUYER, "Buyer not found or invalid role.");

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        if (product.getStock() <= 0) {
            throw new IllegalArgumentException("Product is out of stock.");
        }

        product.setStock(product.getStock() - 1);
        productRepository.save(product);

        Order order = Order.builder()
                .buyer(buyer)
                .createdAt(LocalDateTime.now())
                .status(OrderStatus.PENDING.name())
                .build();
        Order savedOrder = orderRepository.save(order);

        OrderItem item = OrderItem.builder()
                .order(savedOrder)
                .product(product)
                .quantity(1)
                .price(product.getPrice())
                .build();
        orderItemRepository.save(item);

        savedOrder.setItems(List.of(item));
        return toResponseDto(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrdersForBuyer(Long buyerId) {
        return orderRepository.findByBuyerId(buyerId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrdersForSeller(Long sellerId) {
        findUserWithRole(sellerId, RoleType.ROLE_SELLER, "Seller not found or invalid role.");

        return orderRepository.findAll().stream()
                .filter(order -> order.getItems() != null && order.getItems().stream()
                        .anyMatch(item -> item.getProduct() != null
                                && item.getProduct().getSeller() != null
                                && sellerId.equals(item.getProduct().getSeller().getId())))
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    private User findUserWithRole(Long userId, RoleType expectedRole, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(message));

        boolean hasRole = user.getRoles().stream()
                .anyMatch(role -> expectedRole.name().equals(role.getName()));

        if (!hasRole) {
            throw new IllegalArgumentException(message);
        }

        return user;
    }

    private OrderResponseDto toResponseDto(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return OrderResponseDto.builder()
                    .id(order.getId())
                    .buyerId(order.getBuyer() != null ? order.getBuyer().getId() : null)
                    .buyerUsername(order.getBuyer() != null ? order.getBuyer().getName() : null)
                    .status(parseStatus(order.getStatus()))
                    .totalAmount(BigDecimal.ZERO)
                    .orderDate(order.getCreatedAt())
                    .build();
        }

        OrderItem firstItem = order.getItems().get(0);
        Product firstProduct = firstItem.getProduct();

        BigDecimal total = order.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return OrderResponseDto.builder()
                .id(order.getId())
                .buyerId(order.getBuyer() != null ? order.getBuyer().getId() : null)
                .buyerUsername(order.getBuyer() != null ? order.getBuyer().getName() : null)
                .productId(firstProduct != null ? firstProduct.getId() : null)
                .productName(firstProduct != null ? firstProduct.getName() : null)
                .sellerUsername(firstProduct != null && firstProduct.getSeller() != null ? firstProduct.getSeller().getName() : null)
                .status(parseStatus(order.getStatus()))
                .totalAmount(total)
                .orderDate(order.getCreatedAt())
                .build();
    }

    private OrderStatus parseStatus(String status) {
        try {
            return OrderStatus.valueOf(status);
        } catch (Exception ignored) {
            return OrderStatus.PENDING;
        }
    }
}
