package com.dms.controller;

import com.dms.dto.*;
import com.dms.entity.Role;
import com.dms.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final AuthService authService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UserDto>> getAll() { return ResponseEntity.ok(authService.getAllUsers()); }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(authService.getUserById(id));
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UserDto>> byRole(@PathVariable String role) {
        return ResponseEntity.ok(authService.getUsersByRole(Role.valueOf(role)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<AuthResponse> create(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserDto> update(@PathVariable Long id, @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.updateUser(id, req));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> toggleStatus(@PathVariable Long id) {
        authService.toggleUserStatus(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Long>> stats() {
        return ResponseEntity.ok(Map.of("total", authService.countUsers(),
                "admins", authService.countByRole(Role.ADMIN),
                "retailers", authService.countByRole(Role.RETAILER),
                "delivery", authService.countByRole(Role.DELIVERY)));
    }
}
