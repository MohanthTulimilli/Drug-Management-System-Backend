package com.dms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class BulkUploadResult {
    private int totalRecords;
    private int successCount;
    private int failedCount;
    private List<String> errors;
}

