package com.dms.controller;

import com.dms.dto.CheckoutRequest;
import com.dms.entity.Order;
import com.dms.entity.User;
import com.dms.repository.UserRepository;
import com.dms.security.JwtService;
import com.dms.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final OrderService orderService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    private User requireAuth(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new PaymentAuthException("Missing or invalid authorization");
        }
        String token = auth.substring(7).trim();
        if (token.isEmpty() || !jwtService.validateToken(token)) {
            throw new PaymentAuthException("Invalid or expired token");
        }
        String userIdStr = jwtService.getUsernameFromToken(token);
        Long userId = null;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException ignored) {}
        if (userId == null) return null;
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isEnabled()) {
            throw new PaymentAuthException("User not found or disabled");
        }
        return user;
    }

    @PostMapping("/complete")
    public ResponseEntity<Order> completeCheckout(
            @Valid @RequestBody CheckoutRequest request,
            HttpServletRequest httpRequest) {
        User user = requireAuth(httpRequest);
        String retailerName = request.retailerName();
        if (retailerName == null || retailerName.isBlank()) {
            retailerName = (user.getFirstName() != null ? user.getFirstName() : "").trim()
                    + " " + (user.getLastName() != null ? user.getLastName() : "").trim();
            if (retailerName.trim().isEmpty()) retailerName = user.getEmail();
        }
        CheckoutRequest safeRequest = new CheckoutRequest(
                user.getId(),
                retailerName.trim(),
                request.shippingAddress(),
                request.notes(),
                request.shopName(),
                request.contactPhone(),
                request.items()
        );
        Order order = orderService.createOrderFromCheckout(safeRequest);
        return ResponseEntity.ok(order);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class PaymentAuthException extends RuntimeException {
        public PaymentAuthException(String message) {
            super(message);
        }
    }
}
