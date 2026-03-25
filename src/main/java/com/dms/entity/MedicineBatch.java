package com.dms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "medicine_batches")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MedicineBatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(nullable = false, unique = true)
    private String batchNumber;

    private int quantityReceived;
    private int quantityAvailable;
    private int quantitySold;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private String storageLocation;

    @Builder.Default
    private String status = "ACTIVE";

    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
