package com.dms.dto;

public record RetailerAdminDto(
        Long id,
        Long userId,
        String storeName,
        String ownerName,
        String email,
        String phone,
        String address,
        String city,
        boolean verified,
        boolean active,
        boolean userEnabled
) {}

