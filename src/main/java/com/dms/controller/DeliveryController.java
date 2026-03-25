package com.dms.controller;

import com.dms.entity.Delivery;
import com.dms.entity.Delivery.DeliveryStatus;
import com.dms.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {
    private final DeliveryService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Delivery>> getAll(@RequestParam(required = false) String status) {
        if (status != null) return ResponseEntity.ok(service.getByStatus(DeliveryStatus.valueOf(status)));
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Delivery> getById(@PathVariable Long id) { return ResponseEntity.ok(service.getById(id)); }

    @GetMapping("/assigned/{personId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Delivery>> getByPerson(@PathVariable Long personId) {
        return ResponseEntity.ok(service.getByPerson(personId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Delivery> create(@RequestBody Delivery d) { return ResponseEntity.ok(service.create(d)); }

    @PutMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Delivery> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.updateStatus(id, body.get("status"), body.get("notes")));
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> stats() { return ResponseEntity.ok(service.getStats()); }
}
