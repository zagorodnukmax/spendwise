package com.spendwise.transaction;

import com.spendwise.category.Category;
import com.spendwise.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions", indexes = @Index(columnList = "user_id, date"))
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User owner;

    protected Transaction() {
    }

    public Transaction(User owner, BigDecimal amount, TransactionType type, String description,
                       LocalDate date, Category category) {
        this.owner = owner;
        update(amount, type, description, date, category);
    }

    public void update(BigDecimal amount, TransactionType type, String description, LocalDate date, Category category) {
        this.amount = amount;
        this.type = type;
        this.description = description.strip();
        this.date = date;
        this.category = category;
    }

    public Long getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public String getDescription() { return description; }
    public LocalDate getDate() { return date; }
    public Category getCategory() { return category; }
    public User getOwner() { return owner; }
}
