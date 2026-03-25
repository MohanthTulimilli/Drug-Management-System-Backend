package com.dms.service;

import com.dms.entity.Medicine;
import com.dms.entity.MedicineBatch;
import com.dms.entity.Role;
import com.dms.entity.User;
import com.dms.repository.BatchRepository;
import com.dms.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final MedicineRepository medicineRepo;
    private final BatchRepository batchRepo;

    // ---- Medicines (only those with at least one batch, so deleted-from-inventory medicines are hidden) ----
    public List<Medicine> getAllMedicines() { return medicineRepo.findActiveMedicinesWithBatches(); }
    public List<Medicine> getActiveMedicines() { return medicineRepo.findActiveMedicinesWithBatches(); }
    public Medicine getMedicine(Long id) { return medicineRepo.findById(id).orElseThrow(() -> new RuntimeException("Medicine not found")); }
    public List<Medicine> searchMedicines(String q) { return medicineRepo.findByNameContainingIgnoreCase(q); }

    public Medicine createMedicine(Medicine m, User createdBy) {
        m.setCreatedBy(createdBy);
        return medicineRepo.save(m);
    }
    public Medicine updateMedicine(Long id, Medicine data) {
        Medicine m = getMedicine(id);
        m.setName(data.getName()); m.setGenericName(data.getGenericName());
        m.setManufacturer(data.getManufacturer()); m.setCategory(data.getCategory());
        m.setDescription(data.getDescription()); m.setDosageForm(data.getDosageForm());
        m.setStrength(data.getStrength()); m.setUnitPrice(data.getUnitPrice());
        m.setPrescriptionRequired(data.isPrescriptionRequired()); m.setActive(data.isActive());
        return medicineRepo.save(m);
    }
    public void deleteMedicine(Long id) { Medicine m = getMedicine(id); m.setActive(false); medicineRepo.save(m); }

    // ---- Batches ----
    public List<MedicineBatch> getAllBatches() { return batchRepo.findAll(); }
    public List<MedicineBatch> getBatchesByMedicine(Long medicineId) { return batchRepo.findByMedicineId(medicineId); }
    public MedicineBatch getBatch(Long id) { return batchRepo.findById(id).orElseThrow(() -> new RuntimeException("Batch not found")); }

    public MedicineBatch createBatch(MedicineBatch b, Long medicineId) {
        Medicine m = getMedicine(medicineId);
        b.setMedicine(m);
        b.setQuantityAvailable(b.getQuantityReceived());
        b.setQuantitySold(0);
        return batchRepo.save(b);
    }

    public MedicineBatch updateBatch(Long id, MedicineBatch data) {
        MedicineBatch b = getBatch(id);
        if (data.getQuantityReceived() > 0) {
            int diff = data.getQuantityReceived() - b.getQuantityReceived();
            b.setQuantityReceived(data.getQuantityReceived());
            b.setQuantityAvailable(b.getQuantityAvailable() + diff);
        }
        if (data.getSellingPrice() != null) b.setSellingPrice(data.getSellingPrice());
        if (data.getStorageLocation() != null) b.setStorageLocation(data.getStorageLocation());
        if (data.getStatus() != null) b.setStatus(data.getStatus());
        return batchRepo.save(b);
    }

    @Transactional
    public void deleteBatchesByIds(List<Long> batchIds) {
        if (batchIds == null || batchIds.isEmpty()) return;
        batchRepo.deleteAllById(batchIds);
    }

    public List<MedicineBatch> getLowStock() { return batchRepo.findLowStock(); }
    public List<MedicineBatch> getExpiringSoon() { return batchRepo.findExpiringSoon(LocalDate.now().plusDays(30)); }

    // ---- Dashboard ----
    public Map<String, Object> getDashboardStatsForUser(User user) {
        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isSuperAdmin = user.getRole() == Role.SUPER_ADMIN;

        if (isAdmin) {
            long totalMedicines = medicineRepo.countByActiveTrueAndCreatedBy(user);
            long totalBatches = batchRepo.findByAdmin(user).size();
            long totalStock = batchRepo.totalAvailableStockByAdmin(user);
            int lowStockCount = batchRepo.findLowStockByAdmin(user).size();
            int expiringSoonCount = batchRepo.findExpiringSoonByAdmin(user, LocalDate.now().plusDays(30)).size();
            return Map.of(
                    "totalMedicines", totalMedicines,
                    "totalBatches", totalBatches,
                    "totalStock", totalStock,
                    "lowStockCount", lowStockCount,
                    "expiringSoonCount", expiringSoonCount
            );
        }

        // SUPER_ADMIN and other roles see global view
        return Map.of(
                "totalMedicines", medicineRepo.countByActiveTrue(),
                "totalBatches", batchRepo.count(),
                "totalStock", batchRepo.totalAvailableStock(),
                "lowStockCount", batchRepo.findLowStock().size(),
                "expiringSoonCount", batchRepo.findExpiringSoon(LocalDate.now().plusDays(30)).size()
        );
    }

    public List<Medicine> getMedicinesForUser(User user, String search) {
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (isAdmin) {
            List<Medicine> base = medicineRepo.findActiveMedicinesWithBatchesByCreatedBy(user);
            if (search == null || search.isBlank()) return base;
            String q = search.toLowerCase();
            return base.stream()
                    .filter(m -> m.getName() != null && m.getName().toLowerCase().contains(q))
                    .toList();
        }

        // For SUPER_ADMIN, RETAILER, DELIVERY use global catalog (only medicines with batches)
        if (search != null && !search.isBlank()) {
            return medicineRepo.findActiveMedicinesWithBatches().stream()
                    .filter(m -> m.getName() != null && m.getName().toLowerCase().contains(search.toLowerCase()))
                    .toList();
        }
        return medicineRepo.findActiveMedicinesWithBatches();
    }
}
