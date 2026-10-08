package com.example.containermanagement.service;

import com.example.containermanagement.dto.ImportResult;
import com.example.containermanagement.entity.Container;
import com.example.containermanagement.exception.ResourceNotFoundException;
import com.example.containermanagement.repository.ContainerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of ContainerService.
 *
 * Owns the one core business rule of this application: volume is always
 * derived as length x width x height on the backend, and is recalculated
 * on every add and every update. Callers can never set volume directly.
 *
 * Also owns search (delegates to repository finder methods) and Excel
 * export (built in-memory with Apache POI, never written to disk).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContainerServiceImpl implements ContainerService {

    private static final String[] EXCEL_HEADERS = {
            "ID", "Truck Number", "Vendor Code", "Length (m)", "Width (m)", "Height (m)", "Volume (m3)"
    };

    private final ContainerRepository containerRepository;

    @Override
    @Transactional
    public Container addContainer(Container container) {
        container.setVolume(calculateVolume(container));
        Container saved = containerRepository.save(container);
        log.info("Container added: id={}, truckNumber={}, volume={}", saved.getId(), saved.getTruckNumber(), saved.getVolume());
        return saved;
    }

    @Override
    public List<Container> getAllContainers() {
        return containerRepository.findAll();
    }

    @Override
    public Container getContainerById(Long id) {
        return containerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Container not found with id: " + id));
    }

    @Override
    @Transactional
    public Container updateContainer(Long id, Container updatedData) {
        Container existing = getContainerById(id);

        existing.setTruckNumber(updatedData.getTruckNumber());
        existing.setVendorCode(updatedData.getVendorCode());
        existing.setLength(updatedData.getLength());
        existing.setWidth(updatedData.getWidth());
        existing.setHeight(updatedData.getHeight());

        // Volume is never taken from updatedData - always recalculated here.
        existing.setVolume(calculateVolume(existing));

        Container saved = containerRepository.save(existing);
        log.info("Container updated: id={}, volume={}", saved.getId(), saved.getVolume());
        return saved;
    }

    @Override
    @Transactional
    public void deleteContainer(Long id) {
        Container existing = getContainerById(id);
        containerRepository.delete(existing);
        log.info("Container deleted: id={}, truckNumber={}", id, existing.getTruckNumber());
    }

    @Override
    public List<Container> searchByTruckNumber(String truckNumber) {
        return containerRepository.findByTruckNumberContainingIgnoreCase(truckNumber);
    }

    @Override
    public List<Container> searchByVendorCode(String vendorCode) {
        return containerRepository.findByVendorCodeContainingIgnoreCase(vendorCode);
    }

    @Override
    public ByteArrayInputStream exportToExcel() {
        List<Container> containers = containerRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Containers");

            CellStyle headerStyle = createHeaderStyle(workbook);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < EXCEL_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(EXCEL_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (Container container : containers) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(container.getId());
                row.createCell(1).setCellValue(container.getTruckNumber());
                row.createCell(2).setCellValue(container.getVendorCode());
                row.createCell(3).setCellValue(container.getLength().doubleValue());
                row.createCell(4).setCellValue(container.getWidth().doubleValue());
                row.createCell(5).setCellValue(container.getHeight().doubleValue());
                row.createCell(6).setCellValue(container.getVolume().doubleValue());
            }

            for (int i = 0; i < EXCEL_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate Excel export", e);
        }
    }

    @Override
    @Transactional
    public ImportResult importFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Only .xlsx files are supported");
        }

        ImportResult result = new ImportResult();
        List<Container> validContainers = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            // Row 0 is the header (Truck Number, Vendor Code, Length, Width, Height) - skip it.
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                int excelRowNumber = rowIndex + 1; // human-friendly row number for messages

                if (isRowEmpty(row, formatter)) {
                    continue; // silently skip blank rows, not counted as a failure
                }

                try {
                    Container container = parseAndValidateRow(row, formatter);
                    container.setVolume(calculateVolume(container));
                    validContainers.add(container);
                    result.recordSuccess();
                } catch (IllegalArgumentException rowError) {
                    result.recordFailure(excelRowNumber, rowError.getMessage());
                    log.warn("Skipped Excel row {}: {}", excelRowNumber, rowError.getMessage());
                }
            }

            if (!validContainers.isEmpty()) {
                containerRepository.saveAll(validContainers);
            }

            log.info("Excel import complete: {} imported, {} failed", result.getImportedCount(), result.getFailedCount());
            return result;

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded Excel file", e);
        }
    }

    private boolean isRowEmpty(Row row, DataFormatter formatter) {
        if (row == null) return true;
        for (int col = 0; col < 5; col++) {
            if (!getCellText(row, col, formatter).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private Container parseAndValidateRow(Row row, DataFormatter formatter) {
        String truckNumber = getCellText(row, 0, formatter);
        String vendorCode = getCellText(row, 1, formatter);

        if (truckNumber.isEmpty()) {
            throw new IllegalArgumentException("Truck number cannot be empty");
        }
        if (vendorCode.isEmpty()) {
            throw new IllegalArgumentException("Vendor code cannot be empty");
        }

        BigDecimal length = parsePositiveDecimal(getCellText(row, 2, formatter), "Length");
        BigDecimal width = parsePositiveDecimal(getCellText(row, 3, formatter), "Width");
        BigDecimal height = parsePositiveDecimal(getCellText(row, 4, formatter), "Height");

        Container container = new Container();
        container.setTruckNumber(truckNumber);
        container.setVendorCode(vendorCode);
        container.setLength(length);
        container.setWidth(width);
        container.setHeight(height);
        return container;
    }

    private BigDecimal parsePositiveDecimal(String text, String fieldName) {
        if (text.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        BigDecimal value;
        try {
            value = new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid number");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
        return value;
    }

    private String getCellText(Row row, int cellIndex, DataFormatter formatter) {
        Cell cell = row.getCell(cellIndex);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    /**
     * Volume = Length x Width x Height.
     * The single source of truth for this calculation - called on every
     * add and every update so volume can never drift from the dimensions.
     */
    private java.math.BigDecimal calculateVolume(Container container) {
        return container.getLength()
                .multiply(container.getWidth())
                .multiply(container.getHeight());
    }

}
