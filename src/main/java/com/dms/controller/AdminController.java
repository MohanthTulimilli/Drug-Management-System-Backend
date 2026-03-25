package com.dms.controller;

import com.dms.dto.BulkUploadResult;
import com.dms.dto.BulkUploadValidationResult;
import com.dms.entity.AuditLog;
import com.dms.entity.User;
import com.dms.repository.AuditLogRepository;
import com.dms.service.BulkBatchUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AuditLogRepository auditRepo;
    private final BulkBatchUploadService bulkBatchUploadService;

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditRepo.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<AuditLog> createLog(@RequestBody AuditLog log) {
        return ResponseEntity.ok(auditRepo.save(log));
    }

    @GetMapping("/audit-logs/user/{username}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<AuditLog>> getByUser(@PathVariable String username) {
        return ResponseEntity.ok(auditRepo.findByPerformedByOrderByCreatedAtDesc(username));
    }

    @PostMapping("/batch/validate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<BulkUploadValidationResult> validateBatchFile(@RequestPart("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(bulkBatchUploadService.validateFile(file));
    }

    @PostMapping("/batch/upload")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<BulkUploadResult> uploadBatches(
            @AuthenticationPrincipal User user,
            @RequestPart("file") MultipartFile file
    ) throws Exception {
        BulkUploadResult result = bulkBatchUploadService.processFile(file, user);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/inventory/clear")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> clearInventory(@AuthenticationPrincipal User user) {
        bulkBatchUploadService.clearInventoryForAdmin(user);
        return ResponseEntity.ok().build();
    }
}
