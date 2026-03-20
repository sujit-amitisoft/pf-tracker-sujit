package com.amiti.financetracker.bootstrap;

import com.amiti.financetracker.domain.entity.AccountEntity;
import com.amiti.financetracker.domain.entity.BudgetEntity;
import com.amiti.financetracker.domain.entity.CategoryEntity;
import com.amiti.financetracker.domain.entity.GoalEntity;
import com.amiti.financetracker.domain.entity.RecurringTransactionEntity;
import com.amiti.financetracker.domain.entity.TransactionEntity;
import com.amiti.financetracker.domain.entity.UserEntity;
import com.amiti.financetracker.domain.repository.AccountRepository;
import com.amiti.financetracker.domain.repository.BudgetRepository;
import com.amiti.financetracker.domain.repository.CategoryRepository;
import com.amiti.financetracker.domain.repository.GoalRepository;
import com.amiti.financetracker.domain.repository.RecurringTransactionRepository;
import com.amiti.financetracker.domain.repository.TransactionRepository;
import com.amiti.financetracker.domain.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
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
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            BudgetRepository budgetRepository,
            GoalRepository goalRepository,
            RecurringTransactionRepository recurringTransactionRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            String email = "admin@amiti.local";
            if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
                return;
            }

            UserEntity user = new UserEntity();
            user.setEmail(email);
            user.setDisplayName("Admin User");
            user.setPasswordHash(passwordEncoder.encode("Password1"));
            user = userRepository.save(user);

            AccountEntity bank = new AccountEntity();
            bank.setUserId(user.getId());
            bank.setName("Primary Bank");
            bank.setType("BANK_ACCOUNT");
            bank.setInstitutionName("Amiti Bank");
            bank.setOpeningBalance(new BigDecimal("15000.00"));
            bank.setCurrentBalance(new BigDecimal("15000.00"));
            bank = accountRepository.save(bank);

            AccountEntity card = new AccountEntity();
            card.setUserId(user.getId());
            card.setName("Everyday Card");
            card.setType("CREDIT_CARD");
            card.setInstitutionName("Amiti Card Services");
            card.setOpeningBalance(BigDecimal.ZERO);
            card.setCurrentBalance(new BigDecimal("-2400.00"));
            card = accountRepository.save(card);

            Map<String, UUID> categories = categoryRepository.findByUserIdOrUserIdIsNullOrderByNameAsc(user.getId()).stream()
                    .collect(java.util.stream.Collectors.toMap(CategoryEntity::getName, CategoryEntity::getId, (a, b) -> a));

            seedTransaction(transactionRepository, user.getId(), bank.getId(), categories.get("Salary"), "INCOME", "Salary Credit", new BigDecimal("2400.00"), LocalDate.now().minusDays(3), "Monthly salary");
            seedTransaction(transactionRepository, user.getId(), bank.getId(), categories.get("Food"), "EXPENSE", "Fresh Basket", new BigDecimal("220.00"), LocalDate.now().minusDays(2), "Groceries");
            seedTransaction(transactionRepository, user.getId(), card.getId(), categories.get("Transport"), "EXPENSE", "Metro Card", new BigDecimal("85.00"), LocalDate.now().minusDays(1), "Commute recharge");
            seedTransaction(transactionRepository, user.getId(), card.getId(), categories.get("Subscriptions"), "EXPENSE", "Netflix", new BigDecimal("15.99"), LocalDate.now().minusDays(5), "Streaming plan");
            seedTransaction(transactionRepository, user.getId(), bank.getId(), categories.get("Rent"), "EXPENSE", "Apartment Rent", new BigDecimal("650.00"), LocalDate.now().minusDays(7), "Monthly rent");
            seedTransaction(transactionRepository, user.getId(), bank.getId(), categories.get("Food"), "EXPENSE", "Green Grocer", new BigDecimal("96.40"), LocalDate.now().minusDays(8), "Vegetables and fruit");
            seedTransaction(transactionRepository, user.getId(), bank.getId(), categories.get("Utilities"), "EXPENSE", "Power Grid", new BigDecimal("74.20"), LocalDate.now().minusDays(9), "Electricity bill");
            seedTransaction(transactionRepository, user.getId(), bank.getId(), categories.get("Shopping"), "EXPENSE", "Daily Mart", new BigDecimal("48.75"), LocalDate.now().minusDays(10), "Household items");
            seedTransaction(transactionRepository, user.getId(), card.getId(), categories.get("Transport"), "EXPENSE", "Uber", new BigDecimal("11.50"), LocalDate.now().minusDays(11), "Airport commute");
            seedTransaction(transactionRepository, user.getId(), card.getId(), categories.get("Entertainment"), "EXPENSE", "Cineplex", new BigDecimal("32.00"), LocalDate.now().minusDays(12), "Weekend movie");
            seedTransaction(transactionRepository, user.getId(), bank.getId(), categories.get("Health"), "EXPENSE", "Apollo Pharmacy", new BigDecimal("27.35"), LocalDate.now().minusDays(13), "Medicines");
            seedTransaction(transactionRepository, user.getId(), bank.getId(), categories.get("Subscriptions"), "EXPENSE", "Spotify", new BigDecimal("6.99"), LocalDate.now().minusDays(14), "Music subscription");
            seedTransaction(transactionRepository, user.getId(), bank.getId(), categories.get("Food"), "EXPENSE", "Cafe Bloom", new BigDecimal("18.60"), LocalDate.now().minusDays(15), "Coffee meeting");
            seedTransaction(transactionRepository, user.getId(), bank.getId(), categories.get("Salary"), "INCOME", "Freelance Sprint", new BigDecimal("640.00"), LocalDate.now().minusDays(16), "Side project payout");
            seedTransaction(transactionRepository, user.getId(), bank.getId(), categories.get("Travel"), "EXPENSE", "Rail Connect", new BigDecimal("42.00"), LocalDate.now().minusDays(17), "Intercity train");
            seedTransaction(transactionRepository, user.getId(), bank.getId(), categories.get("Food"), "EXPENSE", "Fresh Basket", new BigDecimal("134.25"), LocalDate.now().minusDays(18), "Weekly restock");
            seedTransaction(transactionRepository, user.getId(), bank.getId(), categories.get("Utilities"), "EXPENSE", "FiberNet", new BigDecimal("29.99"), LocalDate.now().minusDays(19), "Internet bill");
            seedTransaction(transactionRepository, user.getId(), card.getId(), categories.get("Shopping"), "EXPENSE", "Urban Threads", new BigDecimal("89.90"), LocalDate.now().minusDays(20), "Clothing");
            seedTransaction(transactionRepository, user.getId(), bank.getId(), categories.get("Gift"), "INCOME", "Birthday Gift", new BigDecimal("150.00"), LocalDate.now().minusDays(21), "Family gift");
            seedTransaction(transactionRepository, user.getId(), bank.getId(), categories.get("Health"), "EXPENSE", "HealthLab", new BigDecimal("58.00"), LocalDate.now().minusDays(22), "Blood test");

            seedBudget(budgetRepository, user.getId(), categories.get("Food"), new BigDecimal("800.00"));
            seedBudget(budgetRepository, user.getId(), categories.get("Transport"), new BigDecimal("250.00"));
            seedBudget(budgetRepository, user.getId(), categories.get("Subscriptions"), new BigDecimal("60.00"));

            GoalEntity goal = new GoalEntity();
            goal.setUserId(user.getId());
            goal.setName("Emergency Fund");
            goal.setTargetAmount(new BigDecimal("10000.00"));
            goal.setCurrentAmount(new BigDecimal("4200.00"));
            goal.setTargetDate(LocalDate.now().plusMonths(8));
            goal.setLinkedAccountId(bank.getId());
            goal.setColor("teal");
            goal.setStatus("ACTIVE");
            goalRepository.save(goal);

            RecurringTransactionEntity recurring = new RecurringTransactionEntity();
            recurring.setUserId(user.getId());
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

            RecurringTransactionEntity salary = new RecurringTransactionEntity();
            salary.setUserId(user.getId());
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
        };
    }

    private void seedTransaction(TransactionRepository repository, UUID userId, UUID accountId, UUID categoryId, String type, String merchant, BigDecimal amount, LocalDate date, String note) {
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
        entity.setTags("");
        repository.save(entity);
    }

    private void seedBudget(BudgetRepository repository, UUID userId, UUID categoryId, BigDecimal amount) {
        if (categoryId == null) {
            return;
        }
        BudgetEntity entity = new BudgetEntity();
        entity.setUserId(userId);
        entity.setCategoryId(categoryId);
        entity.setMonth(LocalDate.now().getMonthValue());
        entity.setYear(LocalDate.now().getYear());
        entity.setAmount(amount);
        entity.setAlertThresholdPercent(80);
        repository.save(entity);
    }
}

