package com.dms.controller;

import com.dms.entity.Order;
import com.dms.entity.Order.OrderStatus;
import com.dms.entity.Role;
import com.dms.entity.User;
import com.dms.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Order>> getAll(
            @RequestParam(required = false) Long retailerId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal User user) {
        if (retailerId != null) {
            if (user.getRole() == Role.RETAILER && !retailerId.equals(user.getId()))
                throw new AccessDeniedException("You can only view your own orders");
            return ResponseEntity.ok(service.getOrdersByRetailer(retailerId));
        }
        if (status != null) {
            if (user.getRole() != Role.ADMIN && user.getRole() != Role.SUPER_ADMIN)
                throw new AccessDeniedException("Only admin can filter by status");
            return ResponseEntity.ok(service.getOrdersByStatus(OrderStatus.valueOf(status)));
        }
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.SUPER_ADMIN)
            throw new AccessDeniedException("Only admin can list all orders");
        return ResponseEntity.ok(service.getAllOrders());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Order> getById(@PathVariable Long id, @AuthenticationPrincipal User user) {
        Order order = service.getOrder(id);
        if (user.getRole() == Role.RETAILER && !Objects.equals(order.getRetailerId(), user.getId()))
            throw new AccessDeniedException("You can only view your own orders");
        return ResponseEntity.ok(order);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Order> create(@RequestBody Order order) { return ResponseEntity.ok(service.createOrder(order)); }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Order> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.updateStatus(id, body.get("status")));
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> stats() { return ResponseEntity.ok(service.getStats()); }
}
