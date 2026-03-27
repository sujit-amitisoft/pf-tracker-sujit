package com.amiti.financetracker.insights.dto;

import java.math.BigDecimal;
import java.util.List;

public class InsightDtos {
    private InsightDtos() {
    }

    public record HealthFactor(String label, int score, String detail) {
    }

    public record HealthScoreResponse(
            int score,
            List<HealthFactor> factors,
            List<String> suggestions
    ) {
    }

    public record InsightCardResponse(
            String title,
            String message,
            String severity
    ) {
    }

    public record InsightSummaryResponse(
            BigDecimal currentMonthSavingsRate,
            BigDecimal previousMonthSavingsRate,
            List<InsightCardResponse> cards
    ) {
    }
}