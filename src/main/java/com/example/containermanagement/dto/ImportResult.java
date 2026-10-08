package com.example.containermanagement.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Summary of an Excel import operation: how many rows were imported
 * successfully, how many were skipped, and why each skipped row failed.
 * Returned to the frontend so the user sees exactly what happened.
 */
@Getter
public class ImportResult {

    private int importedCount = 0;
    private int failedCount = 0;
    private final List<String> failureReasons = new ArrayList<>();

    public void recordSuccess() {
        importedCount++;
    }

    public void recordFailure(int excelRowNumber, String reason) {
        failedCount++;
        failureReasons.add("Row " + excelRowNumber + ": " + reason);
    }

}
