package com.dms.service;

import com.dms.dto.CheckoutRequest;
import com.dms.entity.Order;
import com.dms.entity.Order.OrderStatus;
import com.dms.entity.Order.PaymentStatus;
import com.dms.entity.OrderItem;
import com.dms.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository repo;

    public List<Order> getAllOrders() { return repo.findAllByOrderByCreatedAtDesc(); }
    public List<Order> getOrdersByRetailer(Long retailerId) { return repo.findByRetailerIdOrderByCreatedAtDesc(retailerId); }
    public List<Order> getOrdersByStatus(OrderStatus status) { return repo.findByStatus(status); }
    public Order getOrder(Long id) { return repo.findById(id).orElseThrow(() -> new RuntimeException("Order not found")); }

    public Order createOrder(Order order) {
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setStatus(OrderStatus.PENDING);
        if (order.getPaymentStatus() == null) order.setPaymentStatus(PaymentStatus.PENDING);
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            item.setOrder(order);
            item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            subtotal = subtotal.add(item.getTotalPrice());
        }
        order.setSubtotal(subtotal);
        order.setTax(subtotal.multiply(new BigDecimal("0.05")));
        order.setTotalAmount(subtotal.add(order.getTax()));
        return repo.save(order);
    }

    /** Complete checkout: create order with payment status PAID (no external gateway). */
    public Order createOrderFromCheckout(CheckoutRequest req) {
        Order order = new Order();
        order.setRetailerId(req.retailerId());
        order.setRetailerName(req.retailerName());
        order.setShippingAddress(req.shippingAddress());
        order.setNotes(req.notes());
        order.setShopName(req.shopName());
        order.setContactPhone(req.contactPhone());
        order.setPaymentStatus(PaymentStatus.PAID);
        List<OrderItem> items = new ArrayList<>();
        for (CheckoutRequest.OrderItemPayload p : req.items()) {
            OrderItem item = new OrderItem();
            item.setMedicineId(p.medicineId());
            item.setMedicineName(p.medicineName());
            item.setQuantity(p.quantity());
            item.setUnitPrice(p.unitPrice());
            item.setTotalPrice(p.unitPrice().multiply(BigDecimal.valueOf(p.quantity())));
            item.setOrder(order);
            items.add(item);
        }
        order.setItems(items);
        return createOrder(order);
    }

    public Order updateStatus(Long id, String status) {
        Order order = getOrder(id);
        order.setStatus(OrderStatus.valueOf(status));
        return repo.save(order);
    }

    public Map<String, Object> getStats() {
        return Map.of(
            "total", repo.count(),
            "pending", repo.countByStatus(OrderStatus.PENDING),
            "approved", repo.countByStatus(OrderStatus.APPROVED),
            "shipped", repo.countByStatus(OrderStatus.SHIPPED),
            "delivered", repo.countByStatus(OrderStatus.DELIVERED),
            "cancelled", repo.countByStatus(OrderStatus.CANCELLED)
        );
    }
}
