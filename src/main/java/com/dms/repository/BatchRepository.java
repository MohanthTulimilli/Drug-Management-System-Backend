package com.dms.repository;

import com.dms.entity.MedicineBatch;
import com.dms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface BatchRepository extends JpaRepository<MedicineBatch, Long> {
    List<MedicineBatch> findByMedicineId(Long medicineId);
    List<MedicineBatch> findByStatus(String status);

    java.util.Optional<MedicineBatch> findByBatchNumber(String batchNumber);

    @Query("SELECT b FROM MedicineBatch b WHERE b.quantityAvailable < 10 AND b.status = 'ACTIVE'")
    List<MedicineBatch> findLowStock();

    @Query("SELECT b FROM MedicineBatch b WHERE b.expiryDate <= :date AND b.status = 'ACTIVE'")
    List<MedicineBatch> findExpiringSoon(LocalDate date);

    @Query("SELECT COALESCE(SUM(b.quantityAvailable), 0) FROM MedicineBatch b WHERE b.status = 'ACTIVE'")
    long totalAvailableStock();

    @Query("SELECT b FROM MedicineBatch b WHERE b.medicine.createdBy = :admin")
    List<MedicineBatch> findByAdmin(User admin);

    @Query("SELECT COALESCE(SUM(b.quantityAvailable), 0) FROM MedicineBatch b WHERE b.medicine.createdBy = :admin AND b.status = 'ACTIVE'")
    long totalAvailableStockByAdmin(User admin);

    @Query("SELECT b FROM MedicineBatch b WHERE b.medicine.createdBy = :admin AND b.quantityAvailable < 10 AND b.status = 'ACTIVE'")
    List<MedicineBatch> findLowStockByAdmin(User admin);

    @Query("SELECT b FROM MedicineBatch b WHERE b.medicine.createdBy = :admin AND b.expiryDate <= :date AND b.status = 'ACTIVE'")
    List<MedicineBatch> findExpiringSoonByAdmin(User admin, LocalDate date);
}
