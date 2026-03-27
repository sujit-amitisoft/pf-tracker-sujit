package com.amiti.financetracker.rules.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public class RuleDtos {
    private RuleDtos() {
    }

    public enum RuleField {
        MERCHANT,
        AMOUNT,
        CATEGORY
    }

    public enum RuleOperator {
        EQUALS,
        CONTAINS,
        GREATER_THAN,
        LESS_THAN
    }

    public enum RuleActionType {
        SET_CATEGORY,
        ADD_TAG,
        TRIGGER_ALERT
    }

    public record RuleRequest(
            @NotBlank String name,
            @NotNull RuleField field,
            @NotNull RuleOperator operator,
            @NotBlank String value,
            @NotNull RuleActionType actionType,
            @NotBlank String actionValue,
            @Min(1) @Max(999) Integer priority,
            boolean active
    ) {
    }

    public record RuleResponse(
            UUID id,
            String name,
            RuleField field,
            RuleOperator operator,
            String value,
            RuleActionType actionType,
            String actionValue,
            Integer priority,
            boolean active,
            LocalDateTime createdAt
    ) {
    }
}