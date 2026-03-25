package com.dms.controller;

import com.dms.dto.RetailerAdminDto;
import com.dms.entity.Retailer;
import com.dms.entity.User;
import com.dms.repository.RetailerRepository;
import com.dms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/retailers")
@RequiredArgsConstructor
public class RetailerController {
    private final RetailerRepository repo;
    private final UserRepository userRepo;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Retailer>> getAll() { return ResponseEntity.ok(repo.findByActiveTrue()); }

    /** Admin view: show all retailers + their linked user enabled status. */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<RetailerAdminDto>> getAllAdmin(@AuthenticationPrincipal User admin) {
        List<Retailer> retailers = repo.findAll();
        List<RetailerAdminDto> dto = retailers.stream().map(r -> {
            boolean enabled = false;
            String email = r.getEmail();
            if (r.getUserId() != null) {
                User u = userRepo.findById(r.getUserId()).orElse(null);
                if (u != null) {
                    enabled = u.isEnabled();
                    if (email == null || email.isBlank()) email = u.getEmail();
                }
            }
            return new RetailerAdminDto(
                    r.getId(),
                    r.getUserId(),
                    r.getStoreName(),
                    r.getOwnerName(),
                    email,
                    r.getPhone(),
                    r.getAddress(),
                    r.getCity(),
                    r.isVerified(),
                    r.isActive(),
                    enabled
            );
        }).toList();
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Retailer> getById(@PathVariable Long id) {
        return ResponseEntity.ok(repo.findById(id).orElseThrow(() -> new RuntimeException("Retailer not found")));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Retailer> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(repo.findByUserId(userId).orElseThrow(() -> new RuntimeException("Retailer not found")));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Retailer> create(@RequestBody Retailer r) { return ResponseEntity.ok(repo.save(r)); }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Retailer> update(@PathVariable Long id, @RequestBody Retailer data) {
        Retailer r = repo.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        if (data.getStoreName() != null) r.setStoreName(data.getStoreName());
        if (data.getOwnerName() != null) r.setOwnerName(data.getOwnerName());
        if (data.getAddress() != null) r.setAddress(data.getAddress());
        if (data.getCity() != null) r.setCity(data.getCity());
        if (data.getPhone() != null) r.setPhone(data.getPhone());
        return ResponseEntity.ok(repo.save(r));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Retailer> toggleActive(@PathVariable Long id) {
        Retailer r = repo.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        r.setActive(!r.isActive());
        return ResponseEntity.ok(repo.save(r));
    }

    @PatchMapping("/{id}/toggle-verified")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Retailer> toggleVerified(@PathVariable Long id) {
        Retailer r = repo.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        r.setVerified(!r.isVerified());
        return ResponseEntity.ok(repo.save(r));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Long>> stats() {
        return ResponseEntity.ok(Map.of("total", repo.countByActiveTrue()));
    }
}
