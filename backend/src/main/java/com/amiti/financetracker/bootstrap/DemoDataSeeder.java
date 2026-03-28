package com.amiti.financetracker.bootstrap;

import com.amiti.financetracker.domain.entity.AccountEntity;
import com.amiti.financetracker.domain.entity.AccountMemberEntity;
import com.amiti.financetracker.domain.entity.BudgetEntity;
import com.amiti.financetracker.domain.entity.CategoryEntity;
import com.amiti.financetracker.domain.entity.GoalEntity;
import com.amiti.financetracker.domain.entity.RecurringTransactionEntity;
import com.amiti.financetracker.domain.entity.SharedAccountInviteEntity;
import com.amiti.financetracker.domain.entity.TransactionEntity;
import com.amiti.financetracker.domain.entity.UserEntity;
import com.amiti.financetracker.domain.repository.AccountMemberRepository;
import com.amiti.financetracker.domain.repository.AccountRepository;
import com.amiti.financetracker.domain.repository.BudgetRepository;
import com.amiti.financetracker.domain.repository.CategoryRepository;
import com.amiti.financetracker.domain.repository.GoalRepository;
import com.amiti.financetracker.domain.repository.RecurringTransactionRepository;
import com.amiti.financetracker.domain.repository.SharedAccountInviteRepository;
import com.amiti.financetracker.domain.repository.TransactionRepository;
import com.amiti.financetracker.domain.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoDataSeeder {
    @Bean
    @Order(1)
    CommandLineRunner seedDemoData(
            UserRepository userRepository,
            AccountRepository accountRepository,
            AccountMemberRepository accountMemberRepository,
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            BudgetRepository budgetRepository,
            GoalRepository goalRepository,
            RecurringTransactionRepository recurringTransactionRepository,
            SharedAccountInviteRepository sharedAccountInviteRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            UserEntity user = upsertDemoUser(
                    userRepository,
                    passwordEncoder,
                    "admin@amiti.local",
                    "Admin User",
                    "Password1"
            );
            UserEntity viewerUser = upsertDemoUser(
                    userRepository,
                    passwordEncoder,
                    "viewer@amiti.local",
                    "Viewer Demo",
                    "Password1"
            );

            UUID userId = user.getId();
            AccountEntity bank = findOrCreateAccount(accountRepository, userId, "Primary Bank", () -> {
                AccountEntity entity = new AccountEntity();
                entity.setUserId(userId);
                entity.setName("Primary Bank");
                entity.setType("BANK_ACCOUNT");
                entity.setInstitutionName("Amiti Bank");
                entity.setOpeningBalance(new BigDecimal("4500.00"));
                entity.setCurrentBalance(new BigDecimal("4500.00"));
                return entity;
            });

            AccountEntity card = findOrCreateAccount(accountRepository, userId, "Everyday Card", () -> {
                AccountEntity entity = new AccountEntity();
                entity.setUserId(userId);
                entity.setName("Everyday Card");
                entity.setType("CREDIT_CARD");
                entity.setInstitutionName("Amiti Card Services");
                entity.setOpeningBalance(BigDecimal.ZERO);
                entity.setCurrentBalance(new BigDecimal("-320.00"));
                return entity;
            });

            Map<String, UUID> categories = categoryRepository.findByUserIdOrUserIdIsNullOrderByNameAsc(userId).stream()
                    .collect(java.util.stream.Collectors.toMap(CategoryEntity::getName, CategoryEntity::getId, (a, b) -> a));

            if (transactionRepository.findByAccountIdInOrderByTransactionDateDescCreatedAtDesc(java.util.List.of(bank.getId(), card.getId())).isEmpty()) {
                seedTransaction(transactionRepository, userId, bank.getId(), categories.get("Salary"), "INCOME", "Salary Credit", new BigDecimal("2400.00"), LocalDate.now().minusDays(6), "Monthly salary", "income,payroll");
                seedTransaction(transactionRepository, userId, bank.getId(), categories.get("Rent"), "EXPENSE", "Apartment Rent", new BigDecimal("900.00"), LocalDate.now().minusDays(5), "Monthly rent", "rent");
                seedTransaction(transactionRepository, userId, card.getId(), categories.get("Food"), "EXPENSE", "Fresh Basket", new BigDecimal("120.00"), LocalDate.now().minusDays(3), "Groceries [Alert: Grocery spike]", "groceries,weekly");
                seedTransaction(transactionRepository, userId, card.getId(), categories.get("Transport"), "EXPENSE", "Uber", new BigDecimal("28.00"), LocalDate.now().minusDays(2), "Airport commute", "travel");
                seedTransaction(transactionRepository, userId, bank.getId(), categories.get("Utilities"), "EXPENSE", "Power Grid", new BigDecimal("64.00"), LocalDate.now().minusDays(1), "Electricity bill", "utilities");
            }

            seedBudget(budgetRepository, userId, categories.get("Food"), new BigDecimal("300.00"));
            seedBudget(budgetRepository, userId, categories.get("Transport"), new BigDecimal("120.00"));

            boolean hasEmergencyFund = goalRepository.findByUserIdOrderByTargetDateAsc(userId).stream().anyMatch(goal -> "Emergency Fund".equalsIgnoreCase(goal.getName()));
            if (!hasEmergencyFund) {
                GoalEntity goal = new GoalEntity();
                goal.setUserId(userId);
                goal.setName("Emergency Fund");
                goal.setTargetAmount(new BigDecimal("10000.00"));
                goal.setCurrentAmount(new BigDecimal("4200.00"));
                goal.setTargetDate(LocalDate.now().plusMonths(8));
                goal.setLinkedAccountId(bank.getId());
                goal.setColor("teal");
                goal.setStatus("ACTIVE");
                goalRepository.save(goal);
            }

            boolean hasNetflixRecurring = recurringTransactionRepository.findByUserIdOrderByNextRunDateAsc(userId).stream().anyMatch(item -> "Netflix".equalsIgnoreCase(item.getTitle()));
            if (!hasNetflixRecurring) {
                RecurringTransactionEntity recurring = new RecurringTransactionEntity();
                recurring.setUserId(userId);
                recurring.setTitle("Netflix");
                recurring.setType("EXPENSE");
                recurring.setAmount(new BigDecimal("15.99"));
                recurring.setCategoryId(categories.get("Subscriptions"));
                recurring.setAccountId(card.getId());
                recurring.setFrequency("MONTHLY");
                recurring.setStartDate(LocalDate.now().minusMonths(2));
                recurring.setNextRunDate(LocalDate.now().plusDays(2));
                recurring.setAutoCreateTransaction(true);
                recurring.setPaused(false);
                recurringTransactionRepository.save(recurring);
            }

            boolean hasSalaryRecurring = recurringTransactionRepository.findByUserIdOrderByNextRunDateAsc(userId).stream().anyMatch(item -> "Salary Credit".equalsIgnoreCase(item.getTitle()));
            if (!hasSalaryRecurring) {
                RecurringTransactionEntity salary = new RecurringTransactionEntity();
                salary.setUserId(userId);
                salary.setTitle("Salary Credit");
                salary.setType("INCOME");
                salary.setAmount(new BigDecimal("2400.00"));
                salary.setCategoryId(categories.get("Salary"));
                salary.setAccountId(bank.getId());
                salary.setFrequency("MONTHLY");
                salary.setStartDate(LocalDate.now().minusMonths(6));
                salary.setNextRunDate(LocalDate.now().plusDays(10));
                salary.setAutoCreateTransaction(true);
                salary.setPaused(false);
                recurringTransactionRepository.save(salary);
            }

            removeAccountMember(accountMemberRepository, bank.getId(), viewerUser.getId());
            removeAccountMember(accountMemberRepository, card.getId(), viewerUser.getId());
            seedSharedInvite(
                    sharedAccountInviteRepository,
                    bank.getId(),
                    "viewer@amiti.local",
                    "VIEWER",
                    "demo-shared-invite-token",
                    userId
            );
        };
    }

    private UserEntity upsertDemoUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String email,
            String displayName,
            String password
    ) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseGet(UserEntity::new);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    private AccountEntity findOrCreateAccount(AccountRepository repository, UUID userId, String name, Supplier<AccountEntity> supplier) {
        return repository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .filter(account -> name.equalsIgnoreCase(account.getName()))
                .findFirst()
                .orElseGet(() -> repository.save(supplier.get()));
    }

    private void seedTransaction(TransactionRepository repository, UUID userId, UUID accountId, UUID categoryId, String type, String merchant, BigDecimal amount, LocalDate date, String note, String tags) {
        if (categoryId == null) {
            return;
        }
        TransactionEntity entity = new TransactionEntity();
        entity.setUserId(userId);
        entity.setAccountId(accountId);
        entity.setCategoryId(categoryId);
        entity.setType(type);
        entity.setAmount(amount);
        entity.setTransactionDate(date);
        entity.setMerchant(merchant);
        entity.setNote(note);
        entity.setPaymentMethod("manual");
        entity.setTags(tags);
        repository.save(entity);
    }

    private void seedBudget(BudgetRepository repository, UUID userId, UUID categoryId, BigDecimal amount) {
        if (categoryId == null) {
            return;
        }
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();
        boolean exists = repository.findByUserIdAndMonthAndYear(userId, month, year).stream().anyMatch(item -> categoryId.equals(item.getCategoryId()));
        if (exists) {
            return;
        }
        BudgetEntity entity = new BudgetEntity();
        entity.setUserId(userId);
        entity.setCategoryId(categoryId);
        entity.setMonth(month);
        entity.setYear(year);
        entity.setAmount(amount);
        entity.setAlertThresholdPercent(80);
        repository.save(entity);
    }

    private void removeAccountMember(AccountMemberRepository repository, UUID accountId, UUID userId) {
        repository.findByAccountIdAndUserId(accountId, userId).ifPresent(repository::delete);
    }

    private void seedSharedInvite(
            SharedAccountInviteRepository repository,
            UUID accountId,
            String email,
            String role,
            String token,
            UUID invitedBy
    ) {
        SharedAccountInviteEntity entity = repository.findByToken(token).orElseGet(SharedAccountInviteEntity::new);
        entity.setAccountId(accountId);
        entity.setEmail(email);
        entity.setRole(role);
        entity.setToken(token);
        entity.setInvitedBy(invitedBy);
        entity.setExpiresAt(LocalDateTime.now().plusDays(14));
        entity.setAcceptedAt(null);
        repository.save(entity);
    }
}
