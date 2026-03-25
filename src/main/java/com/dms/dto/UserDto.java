package com.dms.dto;

import java.time.LocalDateTime;

public record UserDto(Long id, String username, String email, String firstName,
                      String lastName, String phone, String role, boolean enabled, LocalDateTime createdAt) {}
