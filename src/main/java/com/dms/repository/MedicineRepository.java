package com.dms.repository;

import com.dms.entity.Medicine;
import com.dms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    List<Medicine> findByActiveTrue();
    List<Medicine> findByCategory(String category);
    List<Medicine> findByNameContainingIgnoreCase(String name);
    long countByActiveTrue();

    List<Medicine> findByCreatedBy(User createdBy);
    List<Medicine> findByActiveTrueAndCreatedBy(User createdBy);
    long countByActiveTrueAndCreatedBy(User createdBy);

    /** Only medicines that have at least one batch (so deleted-from-inventory medicines are not shown). */
    @Query("SELECT m FROM Medicine m WHERE m.active = true AND EXISTS (SELECT 1 FROM MedicineBatch b WHERE b.medicine = m)")
    List<Medicine> findActiveMedicinesWithBatches();

    @Query("SELECT m FROM Medicine m WHERE m.active = true AND m.createdBy = :admin AND EXISTS (SELECT 1 FROM MedicineBatch b WHERE b.medicine = m)")
    List<Medicine> findActiveMedicinesWithBatchesByCreatedBy(User admin);
}
