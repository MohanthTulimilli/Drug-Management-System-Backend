package com.dms.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank String username,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6) String password,
    @NotBlank String firstName,
    @NotBlank String lastName,
    String phone,
    String role
) {}
