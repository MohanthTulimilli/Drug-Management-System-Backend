package com.dms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class BulkUploadValidationResult {
    /** Stage 1: File type is CSV, XLSX, or PDF */
    private boolean stage1FileTypeOk;
    /** Stage 2: Required column names are present */
    private boolean stage2ColumnsOk;
    private String fileType;
    private List<String> requiredColumns;
    private List<String> foundColumns;
    private List<String> missingColumns;
    private List<String> errors;
    private List<List<String>> previewRows;
}
