package com.software.marketplace.service.impl;

import com.software.marketplace.dto.order.OrderCreateRequestDto;
import com.software.marketplace.dto.order.OrderResponseDto;
import com.software.marketplace.entity.Order;
import com.software.marketplace.entity.OrderItem;
import com.software.marketplace.entity.Product;
import com.software.marketplace.entity.User;
import com.software.marketplace.entity.enums.OrderStatus;
import com.software.marketplace.entity.enums.PaymentMethod;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        return placeOrderForBuyer(buyerId, List.of(request));
    }

    @Override
    @Transactional
    public OrderResponseDto placeOrderForBuyer(Long buyerId, List<OrderCreateRequestDto> requests) {
        User buyer = findUserWithRole(buyerId, RoleType.ROLE_BUYER, "Buyer not found or invalid role.");
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("At least one item is required to place an order.");
        }

        Map<Long, Integer> quantityByProductId = new LinkedHashMap<>();
        PaymentMethod selectedPaymentMethod = PaymentMethod.COD;

        for (OrderCreateRequestDto request : requests) {
            if (request.getProductId() == null) {
                throw new IllegalArgumentException("Product id is required.");
            }
            int quantity = normalizeQuantity(request.getQuantity());
            quantityByProductId.merge(request.getProductId(), quantity, Integer::sum);
            if (request.getPaymentMethod() != null) {
                selectedPaymentMethod = request.getPaymentMethod();
            }
        }

        List<Product> products = quantityByProductId.keySet().stream()
                .map(productId -> productRepository.findById(productId)
                        .orElseThrow(() -> new IllegalArgumentException("Product not found.")))
                .toList();

        for (Product product : products) {
            int requestedQty = quantityByProductId.get(product.getId());
            if (product.getStock() < requestedQty) {
                if (requestedQty == 1 && product.getStock() <= 0) {
                    throw new IllegalArgumentException("Product is out of stock.");
                }
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
            }
        }

        for (Product product : products) {
            int requestedQty = quantityByProductId.get(product.getId());
            product.setStock(product.getStock() - requestedQty);
            productRepository.save(product);
        }

        OrderStatus status = selectedPaymentMethod == PaymentMethod.CARD ? OrderStatus.PAID : OrderStatus.PENDING;
        Order order = Order.builder()
                .buyer(buyer)
                .createdAt(LocalDateTime.now())
                .status(status.name())
                .paymentMethod(selectedPaymentMethod.name())
                .build();
        Order savedOrder = orderRepository.save(order);

        List<OrderItem> items = products.stream()
                .map(product -> OrderItem.builder()
                        .order(savedOrder)
                        .product(product)
                        .quantity(quantityByProductId.get(product.getId()))
                        .price(product.getPrice())
                        .build())
                .map(orderItemRepository::save)
                .toList();

        savedOrder.setItems(items);
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

    @Override
    @Transactional
    public void markOrderAsShippedForSeller(Long orderId, Long sellerId) {
        findUserWithRole(sellerId, RoleType.ROLE_SELLER, "Seller not found or invalid role.");

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found."));

        boolean belongsToSeller = order.getItems() != null && order.getItems().stream()
                .anyMatch(item -> item.getProduct() != null
                        && item.getProduct().getSeller() != null
                        && sellerId.equals(item.getProduct().getSeller().getId()));
        if (!belongsToSeller) {
            throw new IllegalArgumentException("You are not authorized to update this order.");
        }

        OrderStatus current = parseStatus(order.getStatus());
        if (current == OrderStatus.SHIPPED) {
            throw new IllegalArgumentException("Order is already shipped.");
        }
        if (current == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled orders cannot be shipped.");
        }

        order.setStatus(OrderStatus.SHIPPED.name());
        orderRepository.save(order);
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
                    .paymentMethod(parsePaymentMethod(order.getPaymentMethod()))
                    .itemCount(0)
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
                .paymentMethod(parsePaymentMethod(order.getPaymentMethod()))
                .itemCount(order.getItems().size())
                .quantity(firstItem.getQuantity())
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

    private PaymentMethod parsePaymentMethod(String paymentMethod) {
        try {
            return paymentMethod == null ? PaymentMethod.COD : PaymentMethod.valueOf(paymentMethod);
        } catch (Exception ignored) {
            return PaymentMethod.COD;
        }
    }

    private int normalizeQuantity(Integer quantity) {
        if (quantity == null) {
            return 1;
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }
        return quantity;
    }
}
