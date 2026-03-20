package com.amiti.financetracker.reports.controller;

import static com.amiti.financetracker.security.SecurityUtils.currentUserId;

import com.amiti.financetracker.reports.dto.ReportDtos.AccountBalanceTrendPoint;
import com.amiti.financetracker.reports.dto.ReportDtos.CategorySpendPoint;
import com.amiti.financetracker.reports.dto.ReportDtos.IncomeExpenseTrendPoint;
import com.amiti.financetracker.reports.service.ReportService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) { this.reportService = reportService; }

    @GetMapping("/category-spend")
    public List<CategorySpendPoint> categorySpend(Authentication authentication) { return reportService.categorySpend(currentUserId(authentication)); }

    @GetMapping("/income-vs-expense")
    public List<IncomeExpenseTrendPoint> incomeVsExpense(Authentication authentication) { return reportService.incomeExpense(currentUserId(authentication)); }

    @GetMapping("/account-balance-trend")
    public List<AccountBalanceTrendPoint> accountBalanceTrend(Authentication authentication) { return reportService.accountBalance(currentUserId(authentication)); }

    @PostMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(Authentication authentication) {
        StringBuilder csv = new StringBuilder("category,amount\n");
        reportService.categorySpend(currentUserId(authentication)).forEach(item -> csv.append(item.category()).append(',').append(item.amount()).append('\n'));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.csv")
                .contentType(new MediaType("text", "csv"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
}
