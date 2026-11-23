package com.spendwise.transaction;

import com.spendwise.category.Category;
import com.spendwise.category.CategoryRepository;
import com.spendwise.category.CategorySuggester;
import com.spendwise.common.NotFoundException;
import com.spendwise.transaction.TransactionDtos.CategoryTotal;
import com.spendwise.transaction.TransactionDtos.TransactionRequest;
import com.spendwise.user.User;
import com.spendwise.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Pure unit tests: repositories are mocked, no Spring context. */
class TransactionServiceTest {

    private static final long USER_ID = 1L;
    private static final YearMonth NOV = YearMonth.of(2026, 11);

    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final TransactionService service =
            new TransactionService(transactions, categories, users, new CategorySuggester());

    private final User user = withId(new User("u@example.com", "hash", "U"), USER_ID);
    private final Category food = withId(new Category(user, "Food", "#16a34a", "silpo, coffee"), 10L);
    private final Category transport = withId(new Category(user, "Transport", "#2563eb", "uber, bolt"), 20L);

    @BeforeEach
    void setUp() {
        when(users.getReferenceById(USER_ID)).thenReturn(user);
        when(categories.findByOwnerIdOrderByName(USER_ID)).thenReturn(List.of(food, transport));
        when(transactions.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---- summary ----

    @Test
    void summaryCalculatesIncomeExpensesAndBalance() {
        givenMonth(
                tx("1000.00", TransactionType.INCOME, null),
                tx("100.50", TransactionType.EXPENSE, food),
                tx("49.50", TransactionType.EXPENSE, transport));

        var summary = service.summary(USER_ID, NOV);

        assertThat(summary.month()).isEqualTo("2026-11");
        assertThat(summary.income()).isEqualByComparingTo("1000.00");
        assertThat(summary.expenses()).isEqualByComparingTo("150.00");
        assertThat(summary.balance()).isEqualByComparingTo("850.00");
    }

    @Test
    void summaryGroupsExpensesByCategorySortedByTotalDesc() {
        givenMonth(
                tx("30", TransactionType.EXPENSE, food),
                tx("200", TransactionType.EXPENSE, transport),
                tx("70", TransactionType.EXPENSE, food),
                tx("5", TransactionType.EXPENSE, null),
                tx("9999", TransactionType.INCOME, food)); // income is not part of the breakdown

        List<CategoryTotal> totals = service.summary(USER_ID, NOV).expensesByCategory();

        assertThat(totals).extracting(CategoryTotal::name).containsExactly("Transport", "Food", "Uncategorized");
        assertThat(totals.get(1).total()).isEqualByComparingTo("100");
        assertThat(totals.get(2).categoryId()).isNull();
    }

    @Test
    void summaryOfEmptyMonthIsAllZeros() {
        givenMonth();
        var summary = service.summary(USER_ID, NOV);
        assertThat(summary.income()).isZero();
        assertThat(summary.expenses()).isZero();
        assertThat(summary.balance()).isZero();
        assertThat(summary.expensesByCategory()).isEmpty();
    }

    @Test
    void summaryQueriesWholeCalendarMonth() {
        givenMonth();
        service.summary(USER_ID, YearMonth.of(2028, 2)); // leap year
        verify(transactions).findByOwnerIdAndDateBetweenOrderByDateDescIdDesc(
                USER_ID, LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29));
    }

    // ---- create / category resolution ----

    @Test
    void createWithoutCategoryAutoCategorizesByDescription() {
        var response = service.create(USER_ID, request("Bolt to the station", null));
        assertThat(response.category().name()).isEqualTo("Transport");
    }

    @Test
    void createWithoutCategoryAndNoMatchStaysUncategorized() {
        var response = service.create(USER_ID, request("Birthday gift", null));
        assertThat(response.category()).isNull();
    }

    @Test
    void explicitCategoryWinsOverKeywords() {
        when(categories.findByIdAndOwnerId(10L, USER_ID)).thenReturn(Optional.of(food));
        var response = service.create(USER_ID, request("Uber eats coffee", 10L));
        assertThat(response.category().name()).isEqualTo("Food");
    }

    @Test
    void categoryOfAnotherUserIsNotFound() {
        when(categories.findByIdAndOwnerId(99L, USER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(USER_ID, request("Anything", 99L)))
                .isInstanceOf(NotFoundException.class);
        verify(transactions, never()).save(any());
    }

    @Test
    void updateOrDeleteOfForeignTransactionIsNotFound() {
        when(transactions.findByIdAndOwnerId(5L, USER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(USER_ID, 5L, request("x", null))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.delete(USER_ID, 5L)).isInstanceOf(NotFoundException.class);
        verify(transactions, never()).delete(any());
    }

    // ---- helpers ----

    private void givenMonth(Transaction... items) {
        when(transactions.findByOwnerIdAndDateBetweenOrderByDateDescIdDesc(eq(USER_ID), any(), any()))
                .thenReturn(List.of(items));
    }

    private Transaction tx(String amount, TransactionType type, Category category) {
        return new Transaction(user, new BigDecimal(amount), type, "t", LocalDate.of(2026, 11, 5), category);
    }

    private static TransactionRequest request(String description, Long categoryId) {
        return new TransactionRequest(new BigDecimal("10.00"), TransactionType.EXPENSE, description,
                LocalDate.of(2026, 11, 5), categoryId);
    }

    private static <T> T withId(T entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
