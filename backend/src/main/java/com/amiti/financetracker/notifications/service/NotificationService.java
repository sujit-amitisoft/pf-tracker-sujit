package com.amiti.financetracker.notifications.service;

import com.amiti.financetracker.budgets.dto.BudgetDtos.BudgetResponse;
import com.amiti.financetracker.budgets.service.BudgetService;
import com.amiti.financetracker.domain.entity.NotificationDismissalEntity;
import com.amiti.financetracker.domain.model.GoalStatus;
import com.amiti.financetracker.domain.repository.NotificationDismissalRepository;
import com.amiti.financetracker.goals.dto.GoalDtos.GoalResponse;
import com.amiti.financetracker.goals.service.GoalService;
import com.amiti.financetracker.notifications.dto.NotificationDtos.NotificationClearRequest;
import com.amiti.financetracker.notifications.dto.NotificationDtos.NotificationResponse;
import com.amiti.financetracker.notifications.dto.NotificationDtos.NotificationSeverity;
import com.amiti.financetracker.recurring.dto.RecurringDtos.RecurringResponse;
import com.amiti.financetracker.recurring.service.RecurringService;
import com.amiti.financetracker.transactions.dto.TransactionDtos.TransactionResponse;
import com.amiti.financetracker.transactions.service.TransactionService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private static final int RECURRING_LOOKAHEAD_DAYS = 3;
    private static final int GOAL_LOOKAHEAD_DAYS = 14;
    private static final Pattern ALERT_PATTERN = Pattern.compile("\\[Alert: (.+?)\\]");

    private final BudgetService budgetService;
    private final RecurringService recurringService;
    private final GoalService goalService;
    private final TransactionService transactionService;
    private final NotificationDismissalRepository notificationDismissalRepository;

    public NotificationService(
            BudgetService budgetService,
            RecurringService recurringService,
            GoalService goalService,
            TransactionService transactionService,
            NotificationDismissalRepository notificationDismissalRepository
    ) {
        this.budgetService = budgetService;
        this.recurringService = recurringService;
        this.goalService = goalService;
        this.transactionService = transactionService;
        this.notificationDismissalRepository = notificationDismissalRepository;
    }

    public List<NotificationResponse> list(UUID userId) {
        List<NotificationCandidate> notifications = buildNotifications(userId);
        if (notifications.isEmpty()) {
            return List.of();
        }

        Set<UUID> dismissedIds = notificationDismissalRepository.findByUserIdAndNotificationIdIn(
                        userId,
                        notifications.stream().map(NotificationCandidate::id).toList()
                ).stream()
                .map(NotificationDismissalEntity::getNotificationId)
                .collect(Collectors.toSet());

        return notifications.stream()
                .filter(notification -> !dismissedIds.contains(notification.id()))
                .sorted(notificationComparator())
                .map(NotificationCandidate::toResponse)
                .toList();
    }

    @Transactional
    public void clear(UUID userId, NotificationClearRequest request) {
        if (request == null || request.ids() == null || request.ids().isEmpty()) {
            return;
        }

        Set<UUID> activeIds = buildNotifications(userId).stream()
                .map(NotificationCandidate::id)
                .collect(Collectors.toSet());
        List<UUID> clearIds = request.ids().stream().filter(activeIds::contains).distinct().toList();
        if (clearIds.isEmpty()) {
            return;
        }

        Set<UUID> existingIds = notificationDismissalRepository.findByUserIdAndNotificationIdIn(userId, clearIds).stream()
                .map(NotificationDismissalEntity::getNotificationId)
                .collect(Collectors.toSet());

        List<NotificationDismissalEntity> dismissals = clearIds.stream()
                .filter(id -> !existingIds.contains(id))
                .map(id -> {
                    NotificationDismissalEntity entity = new NotificationDismissalEntity();
                    entity.setUserId(userId);
                    entity.setNotificationId(id);
                    entity.setDismissedAt(LocalDateTime.now());
                    return entity;
                })
                .toList();

        if (!dismissals.isEmpty()) {
            notificationDismissalRepository.saveAll(dismissals);
        }
    }

    private List<NotificationCandidate> buildNotifications(UUID userId) {
        LocalDate today = LocalDate.now();
        List<NotificationCandidate> notifications = new ArrayList<>();

        for (BudgetResponse budget : budgetService.list(userId, today.getMonthValue(), today.getYear())) {
            int used = budget.percentUsed();
            int threshold = budget.alertThresholdPercent();
            if (used >= 100) {
                notifications.add(candidate(
                        "budget:" + budget.id() + ":" + budget.month() + ":" + budget.year() + ":over",
                        NotificationSeverity.DANGER,
                        budget.category() + " budget is over budget at " + used + "% this month.",
                        "budget",
                        LocalDate.of(budget.year(), budget.month(), 1).atStartOfDay()
                ));
            } else if (used >= threshold) {
                notifications.add(candidate(
                        "budget:" + budget.id() + ":" + budget.month() + ":" + budget.year() + ":threshold",
                        NotificationSeverity.WARNING,
                        budget.category() + " budget is at " + used + "% for this month.",
                        "budget",
                        LocalDate.of(budget.year(), budget.month(), 1).atStartOfDay()
                ));
            }
        }

        for (RecurringResponse recurring : recurringService.list(userId)) {
            if (recurring.paused() || recurring.nextRunDate() == null) {
                continue;
            }
            long daysUntil = ChronoUnit.DAYS.between(today, recurring.nextRunDate());
            if (daysUntil > RECURRING_LOOKAHEAD_DAYS) {
                continue;
            }

            NotificationSeverity severity = daysUntil <= 0 ? NotificationSeverity.WARNING : NotificationSeverity.INFO;
            String timing = daysUntil < 0
                    ? "was due " + Math.abs(daysUntil) + " day" + (Math.abs(daysUntil) == 1 ? "" : "s") + " ago"
                    : daysUntil == 0
                    ? "is due today"
                    : daysUntil == 1
                    ? "is due tomorrow"
                    : "is due in " + daysUntil + " days";
            notifications.add(candidate(
                    "recurring:" + recurring.id() + ":" + recurring.nextRunDate(),
                    severity,
                    recurring.title() + " " + timing + ".",
                    "recurring",
                    recurring.nextRunDate().atStartOfDay()
            ));
        }

        for (GoalResponse goal : goalService.list(userId)) {
            if (goal.targetDate() == null || goal.status() == GoalStatus.COMPLETED || goal.progressPercent() >= 100) {
                continue;
            }
            long daysUntil = ChronoUnit.DAYS.between(today, goal.targetDate());
            if (daysUntil > GOAL_LOOKAHEAD_DAYS) {
                continue;
            }

            NotificationSeverity severity = daysUntil < 0 ? NotificationSeverity.DANGER : NotificationSeverity.WARNING;
            String timing = daysUntil < 0
                    ? "passed its target date and is " + goal.progressPercent() + "% complete"
                    : daysUntil == 0
                    ? "is due today and is " + goal.progressPercent() + "% complete"
                    : daysUntil == 1
                    ? "is due tomorrow and is " + goal.progressPercent() + "% complete"
                    : "is due in " + daysUntil + " days and is " + goal.progressPercent() + "% complete";
            notifications.add(candidate(
                    "goal:" + goal.id() + ":" + goal.targetDate(),
                    severity,
                    goal.name() + " " + timing + ".",
                    "goal",
                    goal.targetDate().atStartOfDay()
            ));
        }

        for (TransactionResponse transaction : transactionService.list(userId)) {
            if (transaction.date() == null || transaction.note() == null || transaction.date().isBefore(today.minusDays(7))) {
                continue;
            }
            Matcher matcher = ALERT_PATTERN.matcher(transaction.note());
            while (matcher.find()) {
                String alert = matcher.group(1);
                notifications.add(candidate(
                        "rule:" + transaction.id() + ":" + alert,
                        NotificationSeverity.WARNING,
                        alert + " for " + (transaction.merchant() == null || transaction.merchant().isBlank() ? transaction.category() : transaction.merchant()) + ".",
                        "rule",
                        transaction.date().atStartOfDay()
                ));
            }
        }

        return notifications;
    }

    private NotificationCandidate candidate(String key, NotificationSeverity severity, String message, String source, LocalDateTime createdAt) {
        UUID id = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
        return new NotificationCandidate(id, severity, message, source, createdAt);
    }

    private Comparator<NotificationCandidate> notificationComparator() {
        return Comparator
                .comparingInt((NotificationCandidate candidate) -> switch (candidate.severity()) {
                    case DANGER -> 0;
                    case WARNING -> 1;
                    case INFO -> 2;
                    case SUCCESS -> 3;
                })
                .thenComparing(NotificationCandidate::createdAt, Comparator.reverseOrder());
    }

    private record NotificationCandidate(
            UUID id,
            NotificationSeverity severity,
            String message,
            String source,
            LocalDateTime createdAt
    ) {
        private NotificationResponse toResponse() {
            return new NotificationResponse(id, severity, message, source, createdAt);
        }
    }
}
