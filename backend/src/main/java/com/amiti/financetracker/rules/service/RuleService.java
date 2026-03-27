package com.amiti.financetracker.rules.service;

import com.amiti.financetracker.common.NotFoundException;
import com.amiti.financetracker.domain.entity.RuleEntity;
import com.amiti.financetracker.domain.repository.CategoryRepository;
import com.amiti.financetracker.domain.repository.RuleRepository;
import com.amiti.financetracker.rules.dto.RuleDtos.RuleActionType;
import com.amiti.financetracker.rules.dto.RuleDtos.RuleField;
import com.amiti.financetracker.rules.dto.RuleDtos.RuleOperator;
import com.amiti.financetracker.rules.dto.RuleDtos.RuleRequest;
import com.amiti.financetracker.rules.dto.RuleDtos.RuleResponse;
import com.amiti.financetracker.transactions.dto.TransactionDtos.TransactionRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuleService {
    private final RuleRepository ruleRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    public RuleService(RuleRepository ruleRepository, CategoryRepository categoryRepository, ObjectMapper objectMapper) {
        this.ruleRepository = ruleRepository;
        this.categoryRepository = categoryRepository;
        this.objectMapper = objectMapper;
    }

    public List<RuleResponse> list(UUID userId) {
        return ruleRepository.findByUserIdOrderByPriorityAscCreatedAtAsc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public RuleResponse create(UUID userId, RuleRequest request) {
        RuleEntity entity = new RuleEntity();
        entity.setUserId(userId);
        apply(entity, request);
        return toResponse(ruleRepository.save(entity));
    }

    @Transactional
    public RuleResponse update(UUID userId, UUID ruleId, RuleRequest request) {
        RuleEntity entity = ruleRepository.findById(ruleId).filter(rule -> userId.equals(rule.getUserId())).orElseThrow(() -> new NotFoundException("Rule not found"));
        apply(entity, request);
        return toResponse(ruleRepository.save(entity));
    }

    @Transactional
    public void delete(UUID userId, UUID ruleId) {
        RuleEntity entity = ruleRepository.findById(ruleId).filter(rule -> userId.equals(rule.getUserId())).orElseThrow(() -> new NotFoundException("Rule not found"));
        ruleRepository.delete(entity);
    }

    public TransactionRequest applyRules(UUID userId, TransactionRequest request) {
        TransactionRequest current = request;
        for (RuleEntity entity : ruleRepository.findByUserIdAndActiveTrueOrderByPriorityAscCreatedAtAsc(userId)) {
            ConditionSpec condition = readCondition(entity.getConditionJson());
            ActionSpec action = readAction(entity.getActionJson());
            if (!matches(current, condition)) {
                continue;
            }
            current = applyAction(userId, current, action);
        }
        return current;
    }

    private boolean matches(TransactionRequest request, ConditionSpec condition) {
        return switch (condition.field()) {
            case MERCHANT -> compareText(request.merchant(), condition.operator(), condition.value());
            case CATEGORY -> compareText(request.categoryId() == null ? null : request.categoryId().toString(), condition.operator(), condition.value());
            case AMOUNT -> compareAmount(request.amount(), condition.operator(), condition.value());
        };
    }

    private boolean compareText(String actual, RuleOperator operator, String expected) {
        String left = actual == null ? "" : actual.trim().toLowerCase();
        String right = expected.trim().toLowerCase();
        return switch (operator) {
            case EQUALS -> left.equals(right);
            case CONTAINS -> left.contains(right);
            default -> false;
        };
    }

    private boolean compareAmount(BigDecimal actual, RuleOperator operator, String expected) {
        BigDecimal right = new BigDecimal(expected.trim());
        return switch (operator) {
            case EQUALS -> actual.compareTo(right) == 0;
            case GREATER_THAN -> actual.compareTo(right) > 0;
            case LESS_THAN -> actual.compareTo(right) < 0;
            default -> false;
        };
    }

    private TransactionRequest applyAction(UUID userId, TransactionRequest request, ActionSpec action) {
        UUID categoryId = request.categoryId();
        List<String> tags = request.tags() == null ? new ArrayList<>() : new ArrayList<>(request.tags());
        String note = request.note();

        switch (action.type()) {
            case SET_CATEGORY -> {
                UUID nextCategoryId = UUID.fromString(action.value());
                categoryRepository.findById(nextCategoryId)
                        .filter(category -> userId.equals(category.getUserId()) || category.getUserId() == null)
                        .orElseThrow(() -> new NotFoundException("Category not found for rule action"));
                categoryId = nextCategoryId;
            }
            case ADD_TAG -> {
                if (tags.stream().noneMatch(tag -> tag.equalsIgnoreCase(action.value()))) {
                    tags.add(action.value());
                }
            }
            case TRIGGER_ALERT -> {
                String marker = "[Alert: " + action.value() + "]";
                if (note == null || note.isBlank()) {
                    note = marker;
                } else if (!note.contains(marker)) {
                    note = note + " " + marker;
                }
            }
        }

        return new TransactionRequest(request.type(), request.amount(), request.date(), request.accountId(), categoryId, request.merchant(), note, request.paymentMethod(), tags);
    }

    private void apply(RuleEntity entity, RuleRequest request) {
        entity.setName(request.name().trim());
        entity.setConditionJson(write(new ConditionSpec(request.field(), request.operator(), request.value().trim())));
        entity.setActionJson(write(new ActionSpec(request.actionType(), request.actionValue().trim())));
        entity.setPriority(request.priority() == null ? 100 : request.priority());
        entity.setActive(request.active());
    }

    private RuleResponse toResponse(RuleEntity entity) {
        ConditionSpec condition = readCondition(entity.getConditionJson());
        ActionSpec action = readAction(entity.getActionJson());
        return new RuleResponse(entity.getId(), entity.getName(), condition.field(), condition.operator(), condition.value(), action.type(), action.value(), entity.getPriority(), entity.isActive(), entity.getCreatedAt());
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize rule", exception);
        }
    }

    private ConditionSpec readCondition(String json) {
        try {
            return objectMapper.readValue(json, ConditionSpec.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read rule condition", exception);
        }
    }

    private ActionSpec readAction(String json) {
        try {
            return objectMapper.readValue(json, ActionSpec.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read rule action", exception);
        }
    }

    private record ConditionSpec(RuleField field, RuleOperator operator, String value) {
    }

    private record ActionSpec(RuleActionType type, String value) {
    }
}