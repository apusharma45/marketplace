package com.software.marketplace.controller;

import com.software.marketplace.dto.order.OrderResponseDto;
import com.software.marketplace.dto.product.ProductResponseDto;
import com.software.marketplace.dto.product.ProductUpsertRequestDto;
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
public class SellerPageController {

    private final ProductService productService;
    private final OrderService orderService;
    private final UserRepository userRepository;

    @GetMapping("/seller")
    public String sellerDashboard(Authentication authentication, Model model) {
        Long sellerId = getCurrentUserId(authentication);
        List<ProductResponseDto> products = productService.getProductsBySeller(sellerId);
        List<OrderResponseDto> orders = orderService.getOrdersForSeller(sellerId);

        long activeListings = products.stream()
                .filter(product -> Boolean.TRUE.equals(product.getActive()))
                .count();
        long lowStockProducts = products.stream()
                .filter(product -> product.getStockQuantity() != null && product.getStockQuantity() <= 5)
                .count();
        BigDecimal totalSales = orders.stream()
                .map(OrderResponseDto::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("productCount", products.size());
        model.addAttribute("activeListings", activeListings);
        model.addAttribute("lowStockProducts", lowStockProducts);
        model.addAttribute("orderCount", orders.size());
        model.addAttribute("totalSales", totalSales);
        model.addAttribute("recentProducts", products.stream()
                .sorted(Comparator.comparing(ProductResponseDto::getUpdatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(5)
                .toList());
        model.addAttribute("recentOrders", orders.stream()
                .sorted(Comparator.comparing(OrderResponseDto::getOrderDate,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(5)
                .toList());

        return "seller/dashboard";
    }

    @GetMapping("/seller/products")
    public String listSellerProducts(Authentication authentication, Model model) {
        Long sellerId = getCurrentUserId(authentication);
        model.addAttribute("products", productService.getProductsBySeller(sellerId));
        return "seller/products/list";
    }

    @GetMapping("/seller/products/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("productRequest")) {
            model.addAttribute("productRequest", new ProductUpsertRequestDto());
        }
        return "seller/products/create";
    }

    @PostMapping("/seller/products/create")
    public String createProduct(
            Authentication authentication,
            @Valid @ModelAttribute("productRequest") ProductUpsertRequestDto request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "seller/products/create";
        }

        try {
            productService.createProductForSeller(getCurrentUserId(authentication), request);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("productError", ex.getMessage());
            return "seller/products/create";
        }

        return "redirect:/seller/products?created";
    }

    @GetMapping("/seller/products/{id}/edit")
    public String showEditForm(@PathVariable("id") Long productId, Authentication authentication, Model model) {
        ProductResponseDto product = productService.getProductForSeller(productId, getCurrentUserId(authentication));
        ProductUpsertRequestDto request = ProductUpsertRequestDto.builder()
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .active(product.getActive())
                .build();

        model.addAttribute("productId", productId);
        model.addAttribute("productRequest", request);
        return "seller/products/edit";
    }

    @PostMapping("/seller/products/{id}/edit")
    public String updateProduct(
            @PathVariable("id") Long productId,
            Authentication authentication,
            @Valid @ModelAttribute("productRequest") ProductUpsertRequestDto request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("productId", productId);
            return "seller/products/edit";
        }

        try {
            productService.updateProductForSeller(productId, getCurrentUserId(authentication), request);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("productId", productId);
            model.addAttribute("productError", ex.getMessage());
            return "seller/products/edit";
        }

        return "redirect:/seller/products?updated";
    }

    @PostMapping("/seller/products/{id}/delete")
    public String deleteProduct(@PathVariable("id") Long productId, Authentication authentication) {
        productService.deleteProductForSeller(productId, getCurrentUserId(authentication));
        return "redirect:/seller/products?deleted";
    }

    @GetMapping("/seller/orders")
    public String sellerOrders(Authentication authentication, Model model) {
        model.addAttribute("orders", orderService.getOrdersForSeller(getCurrentUserId(authentication)));
        return "seller/orders/list";
    }

    private Long getCurrentUserId(Authentication authentication) {
        String login = authentication.getName();
        User user = userRepository.findByName(login)
                .or(() -> userRepository.findByEmail(login))
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found."));
        return user.getId();
    }
}
