package com.software.marketplace.controller;

import com.software.marketplace.dto.order.OrderCreateRequestDto;
import com.software.marketplace.dto.order.OrderResponseDto;
import com.software.marketplace.dto.product.ProductResponseDto;
import com.software.marketplace.entity.User;
import com.software.marketplace.entity.enums.PaymentMethod;
import com.software.marketplace.repository.UserRepository;
import com.software.marketplace.service.OrderService;
import com.software.marketplace.service.ProductService;
import jakarta.servlet.http.HttpSession;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class BuyerPageController {

    private static final String CART_SESSION_KEY = "buyerCart";

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

    @GetMapping("/buyer/cart")
    public String viewCart(HttpSession session, Model model) {
        Map<Long, Integer> cart = getCart(session);
        List<ProductResponseDto> allProducts = productService.getAllAvailableProducts();

        List<CartLine> lines = cart.entrySet().stream()
                .map(entry -> {
                    ProductResponseDto product = allProducts.stream()
                            .filter(p -> Objects.equals(p.getId(), entry.getKey()))
                            .findFirst()
                            .orElse(null);
                    if (product == null) {
                        return null;
                    }
                    int quantity = entry.getValue();
                    BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
                    return new CartLine(product, quantity, lineTotal);
                })
                .filter(Objects::nonNull)
                .toList();

        BigDecimal cartTotal = lines.stream()
                .map(CartLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("cartLines", lines);
        model.addAttribute("cartTotal", cartTotal);
        model.addAttribute("cartSize", cart.values().stream().reduce(0, Integer::sum));
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "buyer/cart";
    }

    @PostMapping("/buyer/cart/add")
    public String addToCart(
            @RequestParam("productId") Long productId,
            @RequestParam(value = "quantity", required = false) Integer quantity,
            HttpSession session
    ) {
        int qty = quantity == null ? 1 : Math.max(1, quantity);
        Map<Long, Integer> cart = getCart(session);
        cart.merge(productId, qty, Integer::sum);
        session.setAttribute(CART_SESSION_KEY, cart);
        return "redirect:/buyer/cart?added";
    }

    @PostMapping("/buyer/cart/remove/{productId}")
    public String removeFromCart(@PathVariable("productId") Long productId, HttpSession session) {
        Map<Long, Integer> cart = getCart(session);
        cart.remove(productId);
        session.setAttribute(CART_SESSION_KEY, cart);
        return "redirect:/buyer/cart?removed";
    }

    @PostMapping("/buyer/cart/increment/{productId}")
    public String incrementCartItem(@PathVariable("productId") Long productId, HttpSession session) {
        Map<Long, Integer> cart = getCart(session);
        if (cart.containsKey(productId)) {
            cart.put(productId, cart.get(productId) + 1);
        }
        session.setAttribute(CART_SESSION_KEY, cart);
        return "redirect:/buyer/cart";
    }

    @PostMapping("/buyer/cart/decrement/{productId}")
    public String decrementCartItem(@PathVariable("productId") Long productId, HttpSession session) {
        Map<Long, Integer> cart = getCart(session);
        if (cart.containsKey(productId)) {
            int next = cart.get(productId) - 1;
            if (next <= 0) {
                cart.remove(productId);
            } else {
                cart.put(productId, next);
            }
        }
        session.setAttribute(CART_SESSION_KEY, cart);
        return "redirect:/buyer/cart";
    }

    @PostMapping("/buyer/cart/checkout")
    public String checkoutCart(
            Authentication authentication,
            @RequestParam(value = "paymentMethod", required = false) PaymentMethod paymentMethod,
            HttpSession session,
            Model model
    ) {
        Map<Long, Integer> cart = getCart(session);
        if (cart.isEmpty()) {
            model.addAttribute("orderError", "Your cart is empty.");
            return viewCart(session, model);
        }

        List<OrderCreateRequestDto> items = cart.entrySet().stream()
                .map(entry -> OrderCreateRequestDto.builder()
                        .productId(entry.getKey())
                        .quantity(entry.getValue())
                        .paymentMethod(paymentMethod == null ? PaymentMethod.COD : paymentMethod)
                        .build())
                .toList();

        try {
            orderService.placeOrderForBuyer(getCurrentUserId(authentication), items);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("orderError", ex.getMessage());
            return viewCart(session, model);
        }

        session.setAttribute(CART_SESSION_KEY, new LinkedHashMap<Long, Integer>());
        return "redirect:/buyer/orders?placed";
    }

    @GetMapping("/buyer/orders/create/{productId}")
    public String showCreateOrderPage(@PathVariable("productId") Long productId, Model model) {
        ProductResponseDto product = productService.getAvailableProductById(productId);
        model.addAttribute("product", product);
        model.addAttribute("orderRequest", OrderCreateRequestDto.builder()
                .productId(productId)
                .quantity(1)
                .paymentMethod(PaymentMethod.COD)
                .build());
        model.addAttribute("paymentMethods", PaymentMethod.values());
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
            model.addAttribute("paymentMethods", PaymentMethod.values());
            return "buyer/orders/create";
        }

        try {
            orderService.placeOrderForBuyer(getCurrentUserId(authentication), request);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("product", productService.getAvailableProductById(request.getProductId()));
            model.addAttribute("orderError", ex.getMessage());
            model.addAttribute("paymentMethods", PaymentMethod.values());
            return "buyer/orders/create";
        }

        return "redirect:/buyer/orders?placed";
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getCart(HttpSession session) {
        Object raw = session.getAttribute(CART_SESSION_KEY);
        if (raw instanceof Map<?, ?> rawMap) {
            Map<Long, Integer> cart = new LinkedHashMap<>();
            rawMap.forEach((key, value) -> {
                if (key instanceof Long productId && value instanceof Integer qty) {
                    cart.put(productId, qty);
                }
            });
            return cart;
        }
        return new LinkedHashMap<>();
    }

    private Long getCurrentUserId(Authentication authentication) {
        String login = authentication.getName();
        User user = userRepository.findByName(login)
                .or(() -> userRepository.findByEmail(login))
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found."));
        return user.getId();
    }

    private record CartLine(ProductResponseDto product, int quantity, BigDecimal lineTotal) {
    }
}
