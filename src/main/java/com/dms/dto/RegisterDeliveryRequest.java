package com.dms.dto;

import jakarta.validation.constraints.*;

public record RegisterDeliveryRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 255)
    String name,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String password
) {}
