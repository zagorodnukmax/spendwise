package com.spendwise.transaction;

import com.spendwise.category.Category;
import com.spendwise.category.CategoryRepository;
import com.spendwise.category.CategorySuggester;
import com.spendwise.common.NotFoundException;
import com.spendwise.transaction.TransactionDtos.*;
import com.spendwise.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

@Service
public class TransactionService {

    private static final String UNCATEGORIZED_COLOR = "#94a3b8";

    private final TransactionRepository transactions;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final CategorySuggester suggester;

    public TransactionService(TransactionRepository transactions, CategoryRepository categories,
                              UserRepository users, CategorySuggester suggester) {
        this.transactions = transactions;
        this.categories = categories;
        this.users = users;
        this.suggester = suggester;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> list(Long userId, YearMonth month) {
        return forMonth(userId, month).stream().map(TransactionResponse::from).toList();
    }

    @Transactional
    public TransactionResponse create(Long userId, TransactionRequest request) {
        var tx = new Transaction(users.getReferenceById(userId), request.amount(), request.type(),
                request.description(), request.date(), resolveCategory(userId, request));
        return TransactionResponse.from(transactions.save(tx));
    }

    @Transactional
    public TransactionResponse update(Long userId, Long id, TransactionRequest request) {
        var tx = transactions.findByIdAndOwnerId(id, userId)
                .orElseThrow(() -> new NotFoundException("Transaction %d not found".formatted(id)));
        tx.update(request.amount(), request.type(), request.description(), request.date(),
                resolveCategory(userId, request));
        return TransactionResponse.from(tx);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        var tx = transactions.findByIdAndOwnerId(id, userId)
                .orElseThrow(() -> new NotFoundException("Transaction %d not found".formatted(id)));
        transactions.delete(tx);
    }

    @Transactional(readOnly = true)
    public MonthlySummary summary(Long userId, YearMonth month) {
        var items = forMonth(userId, month);
        BigDecimal income = sum(items, TransactionType.INCOME);
        BigDecimal expenses = sum(items, TransactionType.EXPENSE);

        var byCategory = new LinkedHashMap<Long, CategoryTotal>();
        items.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .forEach(t -> {
                    Category c = t.getCategory();
                    Long key = c == null ? null : c.getId();
                    byCategory.merge(key,
                            new CategoryTotal(key, c == null ? "Uncategorized" : c.getName(),
                                    c == null ? UNCATEGORIZED_COLOR : c.getColor(), t.getAmount()),
                            (a, b) -> new CategoryTotal(a.categoryId(), a.name(), a.color(), a.total().add(b.total())));
                });
        var totals = byCategory.values().stream()
                .sorted(Comparator.comparing(CategoryTotal::total).reversed())
                .toList();

        return new MonthlySummary(month.toString(), income, expenses, income.subtract(expenses), totals);
    }

    private List<Transaction> forMonth(Long userId, YearMonth month) {
        return transactions.findByOwnerIdAndDateBetweenOrderByDateDescIdDesc(
                userId, month.atDay(1), month.atEndOfMonth());
    }

    private Category resolveCategory(Long userId, TransactionRequest request) {
        if (request.categoryId() != null) {
            return categories.findByIdAndOwnerId(request.categoryId(), userId)
                    .orElseThrow(() -> new NotFoundException("Category %d not found".formatted(request.categoryId())));
        }
        return suggester.suggest(request.description(), categories.findByOwnerIdOrderByName(userId)).orElse(null);
    }

    private static BigDecimal sum(List<Transaction> items, TransactionType type) {
        return items.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
