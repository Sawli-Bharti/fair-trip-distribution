package com.example.fairtripdistribution.controller;

import com.example.fairtripdistribution.model.dto.FairnessReportDto;
import com.example.fairtripdistribution.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Fairness Reports", description = "ADMIN — Generate daily and monthly fairness reports proving vendors received their contracted share of trips")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/daily")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Daily fairness report",
               description = """
                   Returns per-vendor, per-zone, per-trip-type metrics for a single calendar day.
                   Includes: promised %, actual %, actual trips, expected trips, running shortfall (basis points), current capacity snapshot.
                   Results cached for 5 minutes; evicted on new allocations.
                   """,
               parameters = @Parameter(name = "date", description = "Report date (YYYY-MM-DD)", example = "2024-01-15"),
               responses = {
                   @ApiResponse(responseCode = "200", description = "Report generated"),
                   @ApiResponse(responseCode = "403", description = "ADMIN role required")
               })
    public ResponseEntity<List<FairnessReportDto>> getDailyReport(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(reportService.getDailyReport(date));
    }

    @GetMapping("/monthly")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Monthly fairness report",
               description = "Returns aggregated per-vendor metrics across an entire calendar month. Cached for 5 minutes.",
               parameters = {
                   @Parameter(name = "year", description = "4-digit year", example = "2024"),
                   @Parameter(name = "month", description = "Month number (1-12)", example = "1")
               },
               responses = {
                   @ApiResponse(responseCode = "200", description = "Report generated"),
                   @ApiResponse(responseCode = "403", description = "ADMIN role required")
               })
    public ResponseEntity<List<FairnessReportDto>> getMonthlyReport(
            @RequestParam("year") int year,
            @RequestParam("month") int month) {
        return ResponseEntity.ok(reportService.getMonthlyReport(year, month));
    }
}
