package com.amiti.financetracker.insights.controller;

import static com.amiti.financetracker.security.SecurityUtils.currentUserId;

import com.amiti.financetracker.insights.dto.InsightDtos.HealthScoreResponse;
import com.amiti.financetracker.insights.dto.InsightDtos.InsightSummaryResponse;
import com.amiti.financetracker.insights.service.InsightsService;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
public class InsightsController {
    private final InsightsService insightsService;

    public InsightsController(InsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @GetMapping("/health-score")
    public HealthScoreResponse healthScore(Authentication authentication) {
        return insightsService.healthScore(currentUserId(authentication));
    }

    @GetMapping
    public InsightSummaryResponse insights(Authentication authentication,
                                           @RequestParam(required = false) String range,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                           @RequestParam(required = false) UUID accountId,
                                           @RequestParam(required = false) UUID categoryId,
                                           @RequestParam(required = false) String type) {
        return insightsService.insights(currentUserId(authentication), range, startDate, endDate, accountId, categoryId, type);
    }
}