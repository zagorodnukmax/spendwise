package com.spendwise.category;

import com.spendwise.category.CategoryDtos.CategoryRequest;
import com.spendwise.category.CategoryDtos.CategoryResponse;
import com.spendwise.common.ConflictException;
import com.spendwise.common.NotFoundException;
import com.spendwise.transaction.TransactionRepository;
import com.spendwise.user.User;
import com.spendwise.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categories;
    private final UserRepository users;
    private final TransactionRepository transactions;
    private final CategorySuggester suggester;

    public CategoryService(CategoryRepository categories, UserRepository users,
                           TransactionRepository transactions, CategorySuggester suggester) {
        this.categories = categories;
        this.users = users;
        this.transactions = transactions;
        this.suggester = suggester;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(Long userId) {
        return categories.findByOwnerIdOrderByName(userId).stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse create(Long userId, CategoryRequest request) {
        if (categories.existsByOwnerIdAndNameIgnoreCase(userId, request.name().strip())) {
            throw new ConflictException("Category '%s' already exists".formatted(request.name()));
        }
        User owner = users.getReferenceById(userId);
        var saved = categories.save(new Category(owner, request.name(), request.color(), request.keywords()));
        return CategoryResponse.from(saved);
    }

    @Transactional
    public CategoryResponse update(Long userId, Long id, CategoryRequest request) {
        var category = get(userId, id);
        if (!category.getName().equalsIgnoreCase(request.name().strip())
                && categories.existsByOwnerIdAndNameIgnoreCase(userId, request.name().strip())) {
            throw new ConflictException("Category '%s' already exists".formatted(request.name()));
        }
        category.update(request.name(), request.color(), request.keywords());
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        var category = get(userId, id);
        transactions.clearCategory(category.getId());
        categories.delete(category);
    }

    @Transactional(readOnly = true)
    public Optional<CategoryResponse> suggest(Long userId, String description) {
        return suggester.suggest(description, categories.findByOwnerIdOrderByName(userId))
                .map(CategoryResponse::from);
    }

    Category get(Long userId, Long id) {
        return categories.findByIdAndOwnerId(id, userId)
                .orElseThrow(() -> new NotFoundException("Category %d not found".formatted(id)));
    }

    /** Starter set of categories every new user gets. */
    @Transactional
    public List<Category> createDefaults(User owner) {
        return categories.saveAll(List.of(
                new Category(owner, "Groceries", "#16a34a", "silpo, atb, novus, supermarket, market, grocery"),
                new Category(owner, "Cafe & Restaurants", "#ea580c", "coffee, cafe, restaurant, pizza, burger, sushi, lunch"),
                new Category(owner, "Transport", "#2563eb", "uber, bolt, taxi, metro, bus, fuel, wog, okko"),
                new Category(owner, "Subscriptions", "#9333ea", "netflix, spotify, youtube, icloud, chatgpt, claude, github"),
                new Category(owner, "Health", "#dc2626", "pharmacy, apteka, doctor, clinic, dentist, gym"),
                new Category(owner, "Salary", "#0d9488", "salary, payroll, bonus")
        ));
    }
}
