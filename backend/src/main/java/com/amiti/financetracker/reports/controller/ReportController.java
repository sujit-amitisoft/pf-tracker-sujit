package com.amiti.financetracker.reports.controller;

import static com.amiti.financetracker.security.SecurityUtils.currentUserId;

import com.amiti.financetracker.reports.dto.ReportDtos.AccountBalanceTrendPoint;
import com.amiti.financetracker.reports.dto.ReportDtos.CategorySpendPoint;
import com.amiti.financetracker.reports.dto.ReportDtos.IncomeExpenseTrendPoint;
import com.amiti.financetracker.reports.dto.ReportDtos.NetWorthPoint;
import com.amiti.financetracker.reports.dto.ReportDtos.ReportFilter;
import com.amiti.financetracker.reports.dto.ReportDtos.ReportSummaryResponse;
import com.amiti.financetracker.reports.dto.ReportDtos.ReportTransactionRow;
import com.amiti.financetracker.reports.dto.ReportDtos.SavingsRateTrendPoint;
import com.amiti.financetracker.reports.service.ReportService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) { this.reportService = reportService; }

    @GetMapping("/summary")
    public ReportSummaryResponse summary(Authentication authentication, @RequestParam(required = false) String range, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, @RequestParam(required = false) UUID accountId, @RequestParam(required = false) UUID categoryId, @RequestParam(required = false) String type) {
        return reportService.summary(currentUserId(authentication), filter(range, startDate, endDate, accountId, categoryId, type));
    }

    @GetMapping("/transactions")
    public List<ReportTransactionRow> transactions(Authentication authentication, @RequestParam(required = false) String range, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, @RequestParam(required = false) UUID accountId, @RequestParam(required = false) UUID categoryId, @RequestParam(required = false) String type) {
        return reportService.transactions(currentUserId(authentication), filter(range, startDate, endDate, accountId, categoryId, type));
    }

    @GetMapping("/category-spend")
    public List<CategorySpendPoint> categorySpend(Authentication authentication, @RequestParam(required = false) String range, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, @RequestParam(required = false) UUID accountId, @RequestParam(required = false) UUID categoryId, @RequestParam(required = false) String type) {
        return reportService.categorySpend(currentUserId(authentication), filter(range, startDate, endDate, accountId, categoryId, type));
    }

    @GetMapping("/income-vs-expense")
    public List<IncomeExpenseTrendPoint> incomeVsExpense(Authentication authentication, @RequestParam(required = false) String range, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, @RequestParam(required = false) UUID accountId, @RequestParam(required = false) UUID categoryId, @RequestParam(required = false) String type) {
        return reportService.incomeExpense(currentUserId(authentication), filter(range, startDate, endDate, accountId, categoryId, type));
    }

    @GetMapping("/account-balance-trend")
    public List<AccountBalanceTrendPoint> accountBalanceTrend(Authentication authentication, @RequestParam(required = false) String range, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, @RequestParam(required = false) UUID accountId, @RequestParam(required = false) UUID categoryId, @RequestParam(required = false) String type) {
        return reportService.accountBalance(currentUserId(authentication), filter(range, startDate, endDate, accountId, categoryId, type));
    }

    @GetMapping("/trends")
    public List<SavingsRateTrendPoint> trends(Authentication authentication, @RequestParam(required = false) String range, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, @RequestParam(required = false) UUID accountId, @RequestParam(required = false) UUID categoryId) {
        return reportService.savingsRateTrend(currentUserId(authentication), filter(range, startDate, endDate, accountId, categoryId, "ALL"));
    }

    @GetMapping("/net-worth")
    public List<NetWorthPoint> netWorth(Authentication authentication, @RequestParam(required = false) String range, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, @RequestParam(required = false) UUID accountId, @RequestParam(required = false) UUID categoryId) {
        return reportService.netWorth(currentUserId(authentication), filter(range, startDate, endDate, accountId, categoryId, "ALL"));
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(Authentication authentication, @RequestParam(required = false) String range, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, @RequestParam(required = false) UUID accountId, @RequestParam(required = false) UUID categoryId, @RequestParam(required = false) String type) {
        StringBuilder csv = new StringBuilder("date,merchant,category,account,type,amount,note\n");
        reportService.transactions(currentUserId(authentication), filter(range, startDate, endDate, accountId, categoryId, type)).forEach(item ->
                csv.append(item.date()).append(',').append(quoted(item.merchant())).append(',').append(quoted(item.category())).append(',').append(quoted(item.account())).append(',').append(item.type()).append(',').append(item.amount()).append(',').append(quoted(item.note())).append('\n'));
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.csv").contentType(new MediaType("text", "csv")).body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(Authentication authentication, @RequestParam(required = false) String range, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, @RequestParam(required = false) UUID accountId, @RequestParam(required = false) UUID categoryId, @RequestParam(required = false) String type) {
        byte[] pdf = reportService.exportPdf(currentUserId(authentication), filter(range, startDate, endDate, accountId, categoryId, type));
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=finance-report.pdf").contentType(MediaType.APPLICATION_PDF).body(pdf);
    }

    private ReportFilter filter(String range, LocalDate startDate, LocalDate endDate, UUID accountId, UUID categoryId, String type) {
        return new ReportFilter(range == null ? "THIS_MONTH" : range, startDate, endDate, accountId, categoryId, type == null ? "ALL" : type);
    }

    private String quoted(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return '"' + safe + '"';
    }
}