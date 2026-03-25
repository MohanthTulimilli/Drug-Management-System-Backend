package com.dms.repository;

import com.dms.entity.Retailer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RetailerRepository extends JpaRepository<Retailer, Long> {
    Optional<Retailer> findByUserId(Long userId);
    List<Retailer> findByActiveTrue();
    long countByActiveTrue();
}
