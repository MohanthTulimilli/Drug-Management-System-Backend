package com.dms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "retailers")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Retailer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    @Column(nullable = false)
    private String storeName;
    private String ownerName;
    private String licenseNumber;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String phone;
    private String email;
    @Builder.Default
    private boolean verified = false;
    @Builder.Default
    private boolean active = true;
    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @PrePersist
    protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
