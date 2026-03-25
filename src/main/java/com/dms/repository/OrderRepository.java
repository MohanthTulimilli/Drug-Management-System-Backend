package com.dms.repository;

import com.dms.entity.Order;
import com.dms.entity.Order.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByRetailerId(Long retailerId);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByRetailerIdOrderByCreatedAtDesc(Long retailerId);
    List<Order> findAllByOrderByCreatedAtDesc();
    long countByStatus(OrderStatus status);
}
