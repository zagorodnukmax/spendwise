package com.spendwise.config;

import com.spendwise.category.Category;
import com.spendwise.category.CategoryService;
import com.spendwise.transaction.Transaction;
import com.spendwise.transaction.TransactionRepository;
import com.spendwise.transaction.TransactionType;
import com.spendwise.user.User;
import com.spendwise.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Creates a demo account (demo@spendwise.dev / demo12345) with sample data on first start. */
@Component
@ConditionalOnBooleanProperty("app.seed-demo-data")
public class DemoDataLoader implements ApplicationRunner {

    public static final String DEMO_EMAIL = "demo@spendwise.dev";
    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final UserRepository users;
    private final CategoryService categoryService;
    private final TransactionRepository transactions;
    private final PasswordEncoder passwordEncoder;

    public DemoDataLoader(UserRepository users, CategoryService categoryService,
                          TransactionRepository transactions, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.categoryService = categoryService;
        this.transactions = transactions;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByEmailIgnoreCase(DEMO_EMAIL)) {
            return;
        }
        User demo = users.save(new User(DEMO_EMAIL, passwordEncoder.encode("demo12345"), "Demo User"));
        Map<String, Category> c = categoryService.createDefaults(demo).stream()
                .collect(Collectors.toMap(Category::getName, Function.identity()));

        LocalDate today = LocalDate.now();
        LocalDate first = today.withDayOfMonth(1);
        record Row(int dayOffset, String amount, TransactionType type, String description, String category) {
        }
        var rows = new Row[]{
                new Row(0, "45000.00", TransactionType.INCOME, "Monthly salary", "Salary"),
                new Row(1, "1240.50", TransactionType.EXPENSE, "Silpo weekly groceries", "Groceries"),
                new Row(2, "95.00", TransactionType.EXPENSE, "Coffee near office", "Cafe & Restaurants"),
                new Row(3, "199.00", TransactionType.EXPENSE, "Netflix subscription", "Subscriptions"),
                new Row(4, "310.00", TransactionType.EXPENSE, "Uber to airport", "Transport"),
                new Row(5, "860.00", TransactionType.EXPENSE, "Sushi dinner with friends", "Cafe & Restaurants"),
                new Row(6, "420.30", TransactionType.EXPENSE, "Pharmacy", "Health"),
                new Row(7, "2100.00", TransactionType.EXPENSE, "Birthday gift", null),
        };
        for (Row r : rows) {
            LocalDate date = first.plusDays(r.dayOffset());
            if (date.isAfter(today)) {
                date = today;
            }
            transactions.save(new Transaction(demo, new BigDecimal(r.amount()), r.type(), r.description(), date,
                    r.category() == null ? null : c.get(r.category())));
        }
        log.info("Demo account created: {} / demo12345", DEMO_EMAIL);
    }
}
