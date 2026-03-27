package com.amiti.financetracker.forecast.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ForecastDtos {
    private ForecastDtos() {
    }

    public record ForecastMonthResponse(
            BigDecimal currentBalance,
            BigDecimal projectedBalance,
            BigDecimal knownIncome,
            BigDecimal knownExpense,
            BigDecimal averageDailyNet,
            BigDecimal safeToSpend,
            String warning,
            int daysRemaining
    ) {
    }

    public record ForecastDailyPoint(
            LocalDate date,
            BigDecimal projectedBalance
    ) {
    }

    public record ForecastDailyResponse(
            List<ForecastDailyPoint> points
    ) {
    }
}