package com.dms.controller;

import com.dms.entity.Medicine;
import com.dms.entity.MedicineBatch;
import com.dms.entity.User;
import com.dms.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService service;

    // Medicines
    @GetMapping("/medicines")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Medicine>> getMedicines(@AuthenticationPrincipal User user,
                                                       @RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.getMedicinesForUser(user, search));
    }
    @GetMapping("/medicines/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Medicine>> getAllMedicines() { return ResponseEntity.ok(service.getAllMedicines()); }
    @GetMapping("/medicines/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Medicine> getMedicine(@PathVariable Long id) { return ResponseEntity.ok(service.getMedicine(id)); }
    @PostMapping("/medicines")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Medicine> createMedicine(@AuthenticationPrincipal User user, @RequestBody Medicine m) {
        return ResponseEntity.ok(service.createMedicine(m, user));
    }
    @PutMapping("/medicines/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Medicine> updateMedicine(@PathVariable Long id, @RequestBody Medicine m) { return ResponseEntity.ok(service.updateMedicine(id, m)); }
    @DeleteMapping("/medicines/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteMedicine(@PathVariable Long id) { service.deleteMedicine(id); return ResponseEntity.ok().build(); }

    // Batches
    @GetMapping("/batches")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<MedicineBatch>> getBatches(@RequestParam(required = false) Long medicineId) {
        return ResponseEntity.ok(medicineId != null ? service.getBatchesByMedicine(medicineId) : service.getAllBatches());
    }
    @GetMapping("/batches/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<MedicineBatch> getBatch(@PathVariable Long id) { return ResponseEntity.ok(service.getBatch(id)); }
    @PostMapping("/batches")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<MedicineBatch> createBatch(@RequestBody MedicineBatch b, @RequestParam Long medicineId) {
        return ResponseEntity.ok(service.createBatch(b, medicineId));
    }
    @PutMapping("/batches/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<MedicineBatch> updateBatch(@PathVariable Long id, @RequestBody MedicineBatch b) { return ResponseEntity.ok(service.updateBatch(id, b)); }
    @PostMapping("/batches/bulk-delete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteBatchesBulk(@RequestBody List<Long> batchIds) {
        service.deleteBatchesByIds(batchIds);
        return ResponseEntity.ok().build();
    }

    // Alerts & Stats
    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<MedicineBatch>> lowStock() { return ResponseEntity.ok(service.getLowStock()); }
    @GetMapping("/expiring-soon")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<MedicineBatch>> expiring() { return ResponseEntity.ok(service.getExpiringSoon()); }
    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> dashboard(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.getDashboardStatsForUser(user));
    }
}
