package com.dms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CheckoutRequest(
    @NotNull Long retailerId,
    @NotBlank String retailerName,
    @NotBlank String shippingAddress,
    String notes,
    String shopName,
    String contactPhone,
    @NotNull @Size(min = 1) @Valid List<OrderItemPayload> items
) {
    public record OrderItemPayload(
        @NotNull Long medicineId,
        @NotBlank String medicineName,
        @NotNull Integer quantity,
        @NotNull java.math.BigDecimal unitPrice
    ) {}
}
