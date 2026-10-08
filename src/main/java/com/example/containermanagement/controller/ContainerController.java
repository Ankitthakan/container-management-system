package com.example.containermanagement.controller;

import com.example.containermanagement.dto.ImportResult;
import com.example.containermanagement.entity.Container;
import com.example.containermanagement.service.ContainerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * REST controller exposing Container operations.
 *
 * Pure presentation layer: validates incoming requests and delegates
 * everything else to ContainerService. No business logic lives here.
 */
@RestController
@RequestMapping("/api/containers")
@RequiredArgsConstructor
public class ContainerController {

    private final ContainerService containerService;

    // Add Container
    @PostMapping
    public ResponseEntity<Container> addContainer(@Valid @RequestBody Container container) {
        Container saved = containerService.addContainer(container);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // View All Containers
    @GetMapping
    public ResponseEntity<List<Container>> getAllContainers() {
        return ResponseEntity.ok(containerService.getAllContainers());
    }

    // Search by Truck Number OR Vendor Code.
    // Only one is expected per request; if neither is supplied, all
    // containers are returned so the frontend can reuse this endpoint
    // to "reset" after clearing a search.
    @GetMapping("/search")
    public ResponseEntity<List<Container>> searchContainers(
            @RequestParam(required = false) String truckNumber,
            @RequestParam(required = false) String vendorCode) {

        if (truckNumber != null && !truckNumber.isBlank()) {
            return ResponseEntity.ok(containerService.searchByTruckNumber(truckNumber));
        }
        if (vendorCode != null && !vendorCode.isBlank()) {
            return ResponseEntity.ok(containerService.searchByVendorCode(vendorCode));
        }
        return ResponseEntity.ok(containerService.getAllContainers());
    }

    // Export all containers to an Excel (.xlsx) file
    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportToExcel() {
        ByteArrayInputStream excelStream = containerService.exportToExcel();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=containers.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(excelStream));
    }

    // Import containers from an uploaded Excel (.xlsx) file
    @PostMapping("/import")
    public ResponseEntity<ImportResult> importFromExcel(@RequestParam("file") MultipartFile file) {
        ImportResult result = containerService.importFromExcel(file);
        return ResponseEntity.ok(result);
    }

    // View Container by ID
    @GetMapping("/{id}")
    public ResponseEntity<Container> getContainerById(@PathVariable Long id) {
        return ResponseEntity.ok(containerService.getContainerById(id));
    }

    // Update Container
    @PutMapping("/{id}")
    public ResponseEntity<Container> updateContainer(@PathVariable Long id,
                                                       @Valid @RequestBody Container container) {
        Container updated = containerService.updateContainer(id, container);
        return ResponseEntity.ok(updated);
    }

    // Delete Container
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContainer(@PathVariable Long id) {
        containerService.deleteContainer(id);
        return ResponseEntity.noContent().build();
    }

}
