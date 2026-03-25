package com.software.marketplace.controller;

import com.software.marketplace.dto.order.OrderResponseDto;
import com.software.marketplace.dto.product.ProductResponseDto;
import com.software.marketplace.dto.user.UserResponseDto;
import com.software.marketplace.service.OrderService;
import com.software.marketplace.service.ProductService;
import com.software.marketplace.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class AdminPageController {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;

    @GetMapping("/admin")
    public String dashboard(Model model) {
        List<UserResponseDto> users = userService.getAllUsersForAdmin();
        List<ProductResponseDto> products = productService.getAllProductsForAdmin();
        List<OrderResponseDto> orders = orderService.getAllOrders();

        long sellerCount = users.stream()
                .filter(user -> user.getRoles() != null && user.getRoles().contains("ROLE_SELLER"))
                .count();
        long buyerCount = users.stream()
                .filter(user -> user.getRoles() != null && user.getRoles().contains("ROLE_BUYER"))
                .count();
        long activeProducts = products.stream()
                .filter(product -> Boolean.TRUE.equals(product.getActive()))
                .count();
        BigDecimal grossVolume = orders.stream()
                .map(OrderResponseDto::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("userCount", users.size());
        model.addAttribute("sellerCount", sellerCount);
        model.addAttribute("buyerCount", buyerCount);
        model.addAttribute("productCount", products.size());
        model.addAttribute("activeProducts", activeProducts);
        model.addAttribute("orderCount", orders.size());
        model.addAttribute("grossVolume", grossVolume);
        model.addAttribute("recentUsers", users.stream()
                .sorted(Comparator.comparing(UserResponseDto::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(5)
                .toList());
        model.addAttribute("recentProducts", products.stream()
                .sorted(Comparator.comparing(ProductResponseDto::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(5)
                .toList());
        model.addAttribute("recentOrders", orders.stream()
                .sorted(Comparator.comparing(OrderResponseDto::getOrderDate,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(5)
                .toList());
        return "admin/dashboard";
    }

    @GetMapping("/admin/users")
    public String adminUsers(Model model) {
        model.addAttribute("users", userService.getAllUsersForAdmin());
        return "admin/users/list";
    }

    @GetMapping("/admin/products")
    public String adminProducts(Model model) {
        model.addAttribute("products", productService.getAllProductsForAdmin());
        return "admin/products/list";
    }

    @GetMapping("/admin/orders")
    public String adminOrders(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        return "admin/orders/list";
    }
}
