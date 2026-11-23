package com.spendwise.transaction;

import com.spendwise.category.Category;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class TransactionDtos {

    private TransactionDtos() {
    }

    /** If categoryId is null, the backend tries to auto-categorize by description keywords. */
    public record TransactionRequest(
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @NotNull TransactionType type,
            @NotBlank @Size(max = 200) String description,
            @NotNull LocalDate date,
            Long categoryId) {
    }

    public record CategoryRef(Long id, String name, String color) {
        static CategoryRef from(Category c) {
            return c == null ? null : new CategoryRef(c.getId(), c.getName(), c.getColor());
        }
    }

    public record TransactionResponse(Long id, BigDecimal amount, TransactionType type, String description,
                                      LocalDate date, CategoryRef category) {
        static TransactionResponse from(Transaction t) {
            return new TransactionResponse(t.getId(), t.getAmount(), t.getType(), t.getDescription(),
                    t.getDate(), CategoryRef.from(t.getCategory()));
        }
    }

    public record CategoryTotal(Long categoryId, String name, String color, BigDecimal total) {
    }

    public record MonthlySummary(String month, BigDecimal income, BigDecimal expenses, BigDecimal balance,
                                 List<CategoryTotal> expensesByCategory) {
    }
}
