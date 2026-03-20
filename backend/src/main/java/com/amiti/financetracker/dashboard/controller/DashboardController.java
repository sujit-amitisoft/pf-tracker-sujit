package com.amiti.financetracker.dashboard.controller;

import static com.amiti.financetracker.security.SecurityUtils.currentUserId;

import com.amiti.financetracker.dashboard.dto.DashboardDtos.BudgetProgressResponse;
import com.amiti.financetracker.dashboard.dto.DashboardDtos.DashboardSummaryResponse;
import com.amiti.financetracker.dashboard.dto.DashboardDtos.GoalProgressItem;
import com.amiti.financetracker.dashboard.dto.DashboardDtos.RecentTransactionItem;
import com.amiti.financetracker.dashboard.dto.DashboardDtos.RecurringPreviewItem;
import com.amiti.financetracker.dashboard.service.DashboardService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) { this.dashboardService = dashboardService; }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary(Authentication authentication) { return dashboardService.summary(currentUserId(authentication)); }

    @GetMapping("/recent-transactions")
    public List<RecentTransactionItem> recentTransactions(Authentication authentication) { return dashboardService.recent(currentUserId(authentication)); }

    @GetMapping("/upcoming-recurring")
    public List<RecurringPreviewItem> upcomingRecurring(Authentication authentication) { return dashboardService.upcoming(currentUserId(authentication)); }

    @GetMapping("/budget-progress")
    public List<BudgetProgressResponse> budgetProgress(Authentication authentication) { return dashboardService.budgetProgress(currentUserId(authentication)); }

    @GetMapping("/goals-summary")
    public List<GoalProgressItem> goalsSummary(Authentication authentication) { return dashboardService.goalsSummary(currentUserId(authentication)); }
}
