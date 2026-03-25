package com.dms.dto;

public record AuthResponse(String token, Long userId, String username, String email,
                            String firstName, String lastName, String role) {}
