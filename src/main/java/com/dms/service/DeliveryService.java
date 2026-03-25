package com.dms.service;

import com.dms.entity.Delivery;
import com.dms.entity.Delivery.DeliveryStatus;
import com.dms.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeliveryService {
    private final DeliveryRepository repo;

    public List<Delivery> getAll() { return repo.findAllByOrderByCreatedAtDesc(); }
    public Delivery getById(Long id) { return repo.findById(id).orElseThrow(() -> new RuntimeException("Delivery not found")); }
    public List<Delivery> getByPerson(Long personId) { return repo.findByDeliveryPersonIdOrderByCreatedAtDesc(personId); }
    public List<Delivery> getByStatus(DeliveryStatus status) { return repo.findByStatus(status); }

    public Delivery create(Delivery d) { return repo.save(d); }

    public Delivery updateStatus(Long id, String status, String notes) {
        Delivery d = getById(id);
        DeliveryStatus newStatus = DeliveryStatus.valueOf(status);
        d.setStatus(newStatus);
        if (notes != null) d.setNotes(notes);
        if (newStatus == DeliveryStatus.PICKED_UP) d.setPickedUpAt(LocalDateTime.now());
        if (newStatus == DeliveryStatus.DELIVERED) d.setDeliveredAt(LocalDateTime.now());
        return repo.save(d);
    }

    public Map<String, Object> getStats() {
        return Map.of(
            "total", repo.count(),
            "assigned", repo.countByStatus(DeliveryStatus.ASSIGNED),
            "inTransit", repo.countByStatus(DeliveryStatus.IN_TRANSIT),
            "delivered", repo.countByStatus(DeliveryStatus.DELIVERED),
            "failed", repo.countByStatus(DeliveryStatus.FAILED)
        );
    }
}
