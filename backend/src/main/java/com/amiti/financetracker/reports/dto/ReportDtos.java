package com.amiti.financetracker.reports.dto;

import java.math.BigDecimal;

public class ReportDtos {
    private ReportDtos() {
    }

    public record CategorySpendPoint(String category, BigDecimal amount) {
    }

    public record IncomeExpenseTrendPoint(String period, BigDecimal income, BigDecimal expense) {
    }

    public record AccountBalanceTrendPoint(String period, BigDecimal balance) {
    }
}
