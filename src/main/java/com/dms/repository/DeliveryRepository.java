package com.dms.repository;

import com.dms.entity.Delivery;
import com.dms.entity.Delivery.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findByDeliveryPersonIdOrderByCreatedAtDesc(Long personId);
    List<Delivery> findByStatus(DeliveryStatus status);
    List<Delivery> findAllByOrderByCreatedAtDesc();
    long countByStatus(DeliveryStatus status);
    long countByDeliveryPersonId(Long personId);
}
