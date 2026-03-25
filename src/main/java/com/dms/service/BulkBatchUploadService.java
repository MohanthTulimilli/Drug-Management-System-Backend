package com.dms.service;

import com.dms.dto.BulkUploadResult;
import com.dms.dto.BulkUploadValidationResult;
import com.dms.entity.Medicine;
import com.dms.entity.MedicineBatch;
import com.dms.entity.User;
import com.dms.repository.BatchRepository;
import com.dms.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkBatchUploadService {

    private final MedicineRepository medicineRepository;
    private final BatchRepository batchRepository;

    public static final List<String> REQUIRED_COLUMNS = Arrays.asList(
            "medicine_name", "batch_number", "quantity", "expiry_date", "price"
    );

    private static final DateTimeFormatter[] DATE_FORMATS = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
    };

    /** Removes all batches and medicines owned by this admin so uploads can run fresh. */
    public void clearInventoryForAdmin(User admin) {
        List<MedicineBatch> batches = batchRepository.findByAdmin(admin);
        if (!batches.isEmpty()) batchRepository.deleteAll(batches);
        List<Medicine> medicines = medicineRepository.findByCreatedBy(admin);
        if (!medicines.isEmpty()) medicineRepository.deleteAll(medicines);
    }

    public BulkUploadResult processFile(MultipartFile file, User admin) throws IOException {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        List<String> errors = new ArrayList<>();
        int[] counters = new int[]{0, 0}; // [total, success]

        if (filename.endsWith(".csv")) {
            processCsv(file.getInputStream(), admin, counters, errors);
        } else if (filename.endsWith(".xlsx")) {
            processExcel(file.getInputStream(), admin, counters, errors);
        } else if (filename.endsWith(".pdf")) {
            processPdf(file.getInputStream(), admin, counters, errors);
        } else {
            throw new IllegalArgumentException("Unsupported file type. Only CSV, XLSX, and PDF are allowed.");
        }

        int total = counters[0];
        int success = counters[1];
        int failed = total - success;
        return BulkUploadResult.builder()
                .totalRecords(total)
                .successCount(success)
                .failedCount(failed)
                .errors(errors)
                .build();
    }

    /**
     * Stage 1: Check file type. Stage 2: Check required column names present.
     * Does not save anything; returns validation result and preview rows.
     */
    public BulkUploadValidationResult validateFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        List<String> errors = new ArrayList<>();
        List<String> foundColumns = new ArrayList<>();
        List<String> missingColumns = new ArrayList<>(REQUIRED_COLUMNS);
        List<List<String>> previewRows = new ArrayList<>();

        // Stage 1: File type
        String fileType = null;
        if (filename.endsWith(".csv")) fileType = "csv";
        else if (filename.endsWith(".xlsx")) fileType = "xlsx";
        else if (filename.endsWith(".pdf")) fileType = "pdf";

        boolean stage1FileTypeOk = fileType != null;
        if (!stage1FileTypeOk) {
            errors.add("Unsupported file type. Allowed: CSV (.csv), Excel (.xlsx), PDF (.pdf).");
            return BulkUploadValidationResult.builder()
                    .stage1FileTypeOk(false)
                    .stage2ColumnsOk(false)
                    .fileType(filename.isEmpty() ? "unknown" : filename)
                    .requiredColumns(new ArrayList<>(REQUIRED_COLUMNS))
                    .foundColumns(foundColumns)
                    .missingColumns(new ArrayList<>(REQUIRED_COLUMNS))
                    .errors(errors)
                    .previewRows(previewRows)
                    .build();
        }

        // Stage 2: Required columns present
        if ("csv".equals(fileType)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
                 CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreEmptyLines().parse(reader)) {
                Map<String, Integer> headerMap = parser.getHeaderMap();
                foundColumns.addAll(headerMap.keySet().stream().map(this::normalizeColumnName).toList());
                missingColumns = REQUIRED_COLUMNS.stream()
                        .filter(c -> !foundColumns.contains(c))
                        .collect(Collectors.toList());
                int rowCount = 0;
                for (CSVRecord rec : parser) {
                    if (rowCount >= 5) break;
                    List<String> row = new ArrayList<>();
                    for (String h : parser.getHeaderNames()) row.add(rec.get(h).trim());
                    previewRows.add(row);
                    rowCount++;
                }
            }
        } else if ("xlsx".equals(fileType)) {
            try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
                Sheet sheet = workbook.getSheetAt(0);
                if (sheet != null) {
                    Row headerRow = sheet.getRow(0);
                    if (headerRow != null) {
                        for (int i = 0; i < 5; i++) {
                            String val = normalizeColumnName(getCellString(headerRow.getCell(i)));
                            if (!val.isBlank()) foundColumns.add(val);
                        }
                    }
                    missingColumns = REQUIRED_COLUMNS.stream()
                            .filter(c -> !foundColumns.contains(c))
                            .collect(Collectors.toList());
                    for (int r = 1; r <= Math.min(5, sheet.getLastRowNum()); r++) {
                        Row row = sheet.getRow(r);
                        if (row == null) continue;
                        List<String> cells = new ArrayList<>();
                        for (int c = 0; c < 5; c++) cells.add(getCellString(row.getCell(c)));
                        previewRows.add(cells);
                    }
                }
            }
        } else {
            // PDF: expect first data line to have 5 comma-separated values
            try (PDDocument doc = PDDocument.load(file.getInputStream())) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(doc);
                String[] lines = text.split("\\R");
                for (String line : lines) {
                    String t = line.trim();
                    if (t.isEmpty()) continue;
                    String[] parts = t.split(",");
                    if (parts.length >= 5) {
                        foundColumns.addAll(REQUIRED_COLUMNS);
                        missingColumns.clear();
                        for (int i = 0; i < Math.min(5, lines.length); i++) {
                            String l = lines[i].trim();
                            if (l.isEmpty()) continue;
                            String[] p = l.split(",");
                            if (p.length >= 5) previewRows.add(Arrays.asList(p[0].trim(), p[1].trim(), p[2].trim(), p[3].trim(), p[4].trim()));
                        }
                        break;
                    }
                }
                if (missingColumns.isEmpty() && previewRows.isEmpty() && lines.length > 0) {
                    missingColumns.addAll(REQUIRED_COLUMNS);
                    errors.add("PDF: Could not find a line with 5 comma-separated columns (medicine_name, batch_number, quantity, expiry_date, price).");
                }
            }
        }

        boolean stage2ColumnsOk = missingColumns.isEmpty();
        if (!stage2ColumnsOk && errors.isEmpty()) {
            errors.add("Missing required columns: " + String.join(", ", missingColumns) + ". Required: " + String.join(", ", REQUIRED_COLUMNS));
        }

        return BulkUploadValidationResult.builder()
                .stage1FileTypeOk(stage1FileTypeOk)
                .stage2ColumnsOk(stage2ColumnsOk)
                .fileType(fileType)
                .requiredColumns(new ArrayList<>(REQUIRED_COLUMNS))
                .foundColumns(foundColumns)
                .missingColumns(missingColumns)
                .errors(errors)
                .previewRows(previewRows)
                .build();
    }

    private void processCsv(InputStream in, User admin, int[] counters, List<String> errors) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreEmptyLines()
                     .parse(reader)) {
            List<MedicineBatch> toSave = new ArrayList<>();
            Map<String, Medicine> medicineCache = new HashMap<>();
            int rowNum = 1; // header is row 1 visually

            for (CSVRecord record : parser) {
                rowNum++;
                counters[0]++;
                String medicineName = record.get("medicine_name").trim();
                String batchNumber = record.get("batch_number").trim();
                String qtyStr = record.get("quantity").trim();
                String expiryStr = record.get("expiry_date").trim();
                String priceStr = record.get("price").trim();

                String error = validateAndBuildBatch(admin, medicineName, batchNumber, qtyStr, expiryStr, priceStr, rowNum, toSave, medicineCache);
                if (error != null) {
                    errors.add(error);
                } else {
                    counters[1]++;
                }
            }

            if (!toSave.isEmpty()) {
                batchRepository.saveAll(toSave);
            }
        }
    }

    private void processExcel(InputStream in, User admin, int[] counters, List<String> errors) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) return;

            List<MedicineBatch> toSave = new ArrayList<>();
            Map<String, Medicine> medicineCache = new HashMap<>();
            boolean headerSkipped = false;
            for (Row row : sheet) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                counters[0]++;
                int rowNum = row.getRowNum() + 1;

                String medicineName = getCellString(row.getCell(0));
                String batchNumber = getCellString(row.getCell(1));
                String qtyStr = getCellString(row.getCell(2));
                String expiryStr = getCellString(row.getCell(3));
                String priceStr = getCellString(row.getCell(4));

                String error = validateAndBuildBatch(admin, medicineName, batchNumber, qtyStr, expiryStr, priceStr, rowNum, toSave, medicineCache);
                if (error != null) {
                    errors.add(error);
                } else {
                    counters[1]++;
                }
            }

            if (!toSave.isEmpty()) {
                batchRepository.saveAll(toSave);
            }
        }
    }

    private void processPdf(InputStream in, User admin, int[] counters, List<String> errors) throws IOException {
        try (PDDocument document = PDDocument.load(in)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            String[] lines = text.split("\\R");

            List<MedicineBatch> toSave = new ArrayList<>();
            Map<String, Medicine> medicineCache = new HashMap<>();
            int rowNum = 0;
            for (String line : lines) {
                rowNum++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                if (rowNum == 1 && trimmed.toLowerCase(Locale.ROOT).contains("medicine")) {
                    // header
                    continue;
                }
                counters[0]++;
                String[] parts = trimmed.split(",");
                if (parts.length < 5) {
                    errors.add("Row " + rowNum + ": Not enough columns");
                    continue;
                }
                String medicineName = parts[0].trim();
                String batchNumber = parts[1].trim();
                String qtyStr = parts[2].trim();
                String expiryStr = parts[3].trim();
                String priceStr = parts[4].trim();

                String error = validateAndBuildBatch(admin, medicineName, batchNumber, qtyStr, expiryStr, priceStr, rowNum, toSave, medicineCache);
                if (error != null) {
                    errors.add(error);
                } else {
                    counters[1]++;
                }
            }

            if (!toSave.isEmpty()) {
                batchRepository.saveAll(toSave);
            }
        }
    }

    private String validateAndBuildBatch(User admin,
                                         String medicineName,
                                         String batchNumber,
                                         String qtyStr,
                                         String expiryStr,
                                         String priceStr,
                                         int rowNum,
                                         List<MedicineBatch> toSave,
                                         Map<String, Medicine> medicineCache) {
        List<String> rowErrors = new ArrayList<>();

        if (medicineName == null || medicineName.isBlank()) {
            rowErrors.add("medicine_name is required");
        }
        if (batchNumber == null || batchNumber.isBlank()) {
            rowErrors.add("batch_number is required");
        }

        Integer quantity = null;
        try {
            quantity = Integer.parseInt(qtyStr);
            if (quantity <= 0) rowErrors.add("quantity must be positive");
        } catch (Exception e) {
            rowErrors.add("quantity must be a valid number");
        }

        BigDecimal price = null;
        try {
            price = new BigDecimal(priceStr);
            if (price.compareTo(BigDecimal.ZERO) <= 0) rowErrors.add("price must be positive");
        } catch (Exception e) {
            rowErrors.add("price must be a valid number");
        }

        LocalDate expiry = null;
        try {
            expiry = parseDate(expiryStr);
            if (expiry == null || !expiry.isAfter(LocalDate.now())) {
                rowErrors.add("expiry_date must be a future date");
            }
        } catch (Exception e) {
            rowErrors.add("expiry_date is invalid");
        }

        if (batchNumber != null && !batchNumber.isBlank() && batchRepository.findByBatchNumber(batchNumber).isPresent()) {
            rowErrors.add("Duplicate batch number");
        }

        if (!rowErrors.isEmpty()) {
            return "Row " + rowNum + ": " + String.join("; ", rowErrors);
        }

        Medicine medicine = resolveMedicine(admin, medicineName, price, medicineCache);

        MedicineBatch batch = MedicineBatch.builder()
                .medicine(medicine)
                .batchNumber(batchNumber)
                .quantityReceived(quantity)
                .quantityAvailable(quantity)
                .quantitySold(0)
                .costPrice(price)
                .sellingPrice(price)
                .expiryDate(expiry)
                .status("ACTIVE")
                .build();
        toSave.add(batch);

        return null;
    }

    private Medicine resolveMedicine(User admin, String medicineName, BigDecimal unitPrice, Map<String, Medicine> cache) {
        String key = admin.getId() + ":" + medicineName.toLowerCase(Locale.ROOT);
        if (cache.containsKey(key)) return cache.get(key);

        Medicine existing = medicineRepository.findByNameContainingIgnoreCase(medicineName).stream()
                .filter(m -> m.getCreatedBy() != null && m.getCreatedBy().getId().equals(admin.getId()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            cache.put(key, existing);
            return existing;
        }

        Medicine created = Medicine.builder()
                .name(medicineName)
                .unitPrice(unitPrice)
                .createdBy(admin)
                .active(true)
                .build();
        created = medicineRepository.save(created);
        cache.put(key, created);
        return created;
    }

    private String normalizeColumnName(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT).replace(" ", "_");
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}

