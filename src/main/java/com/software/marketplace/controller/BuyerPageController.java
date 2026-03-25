package com.software.marketplace.controller;

import com.software.marketplace.dto.order.OrderCreateRequestDto;
import com.software.marketplace.dto.order.OrderResponseDto;
import com.software.marketplace.dto.product.ProductResponseDto;
import com.software.marketplace.entity.User;
import com.software.marketplace.repository.UserRepository;
import com.software.marketplace.service.OrderService;
import com.software.marketplace.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class BuyerPageController {

    private final ProductService productService;
    private final OrderService orderService;
    private final UserRepository userRepository;

    @GetMapping("/buyer")
    public String buyerDashboard(Authentication authentication, Model model) {
        Long buyerId = getCurrentUserId(authentication);
        List<ProductResponseDto> products = productService.getAllAvailableProducts();
        List<OrderResponseDto> orders = orderService.getOrdersForBuyer(buyerId);

        long inStockProducts = products.stream()
                .filter(product -> product.getStockQuantity() != null && product.getStockQuantity() > 0)
                .count();
        BigDecimal totalSpent = orders.stream()
                .map(OrderResponseDto::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("availableProductCount", products.size());
        model.addAttribute("inStockProducts", inStockProducts);
        model.addAttribute("orderCount", orders.size());
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("featuredProducts", products.stream()
                .sorted(Comparator.comparing(ProductResponseDto::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(4)
                .toList());
        model.addAttribute("recentOrders", orders.stream()
                .sorted(Comparator.comparing(OrderResponseDto::getOrderDate,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(5)
                .toList());
        return "buyer/dashboard";
    }

    @GetMapping("/buyer/products")
    public String listAvailableProducts(Model model) {
        model.addAttribute("products", productService.getAllAvailableProducts());
        return "buyer/products/list";
    }

    @GetMapping("/buyer/products/{id}")
    public String productDetails(@PathVariable("id") Long productId, Model model) {
        model.addAttribute("product", productService.getAvailableProductById(productId));
        return "buyer/products/detail";
    }

    @GetMapping("/buyer/orders")
    public String buyerOrders(Authentication authentication, Model model) {
        model.addAttribute("orders", orderService.getOrdersForBuyer(getCurrentUserId(authentication)));
        return "buyer/orders/list";
    }

    @GetMapping("/buyer/orders/create/{productId}")
    public String showCreateOrderPage(@PathVariable("productId") Long productId, Model model) {
        ProductResponseDto product = productService.getAvailableProductById(productId);
        model.addAttribute("product", product);
        model.addAttribute("orderRequest", OrderCreateRequestDto.builder().productId(productId).build());
        return "buyer/orders/create";
    }

    @PostMapping("/buyer/orders/create")
    public String createOrder(
            Authentication authentication,
            @Valid @ModelAttribute("orderRequest") OrderCreateRequestDto request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("product", productService.getAvailableProductById(request.getProductId()));
            return "buyer/orders/create";
        }

        try {
            orderService.placeOrderForBuyer(getCurrentUserId(authentication), request);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("product", productService.getAvailableProductById(request.getProductId()));
            model.addAttribute("orderError", ex.getMessage());
            return "buyer/orders/create";
        }

        return "redirect:/buyer/orders?placed";
    }

    private Long getCurrentUserId(Authentication authentication) {
        String login = authentication.getName();
        User user = userRepository.findByName(login)
                .or(() -> userRepository.findByEmail(login))
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found."));
        return user.getId();
    }
}
