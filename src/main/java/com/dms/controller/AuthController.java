package com.dms.controller;

import com.dms.dto.*;
import com.dms.entity.User;
import com.dms.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleLoginException(RuntimeException ex) {
        String msg = ex.getMessage();
        if ("Invalid credentials".equals(msg) || "Account disabled".equals(msg))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", msg));
        throw ex;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /** ADMIN only: create RETAILER user. No self-registration. */
    @PostMapping("/register-retailer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> registerRetailer(@Valid @RequestBody RegisterRetailerRequest request) {
        return ResponseEntity.ok(authService.registerRetailer(request));
    }

    /** ADMIN only: create DELIVERY user. No self-registration. */
    @PostMapping("/register-delivery")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> registerDelivery(@Valid @RequestBody RegisterDeliveryRequest request) {
        return ResponseEntity.ok(authService.registerDelivery(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.getUserById(user.getId()));
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("valid", true, "userId", user.getId(),
                "username", user.getUsername(), "role", user.getRole().name()));
    }
}
