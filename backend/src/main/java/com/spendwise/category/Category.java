package com.spendwise.category;

import com.spendwise.user.User;
import jakarta.persistence.*;

import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "categories", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "name"}))
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String name;

    /** Hex color used by the UI, e.g. "#4f46e5". */
    @Column(nullable = false, length = 7)
    private String color;

    /** Comma-separated keywords used to auto-categorize transactions by description. */
    @Column(length = 500)
    private String keywords;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User owner;

    protected Category() {
    }

    public Category(User owner, String name, String color, String keywords) {
        this.owner = owner;
        update(name, color, keywords);
    }

    public void update(String name, String color, String keywords) {
        this.name = name.strip();
        this.color = color;
        this.keywords = keywords == null ? "" : keywords.strip();
    }

    public List<String> keywordList() {
        if (keywords == null || keywords.isBlank()) {
            return List.of();
        }
        return Arrays.stream(keywords.split(","))
                .map(String::strip)
                .map(String::toLowerCase)
                .filter(k -> !k.isEmpty())
                .toList();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public String getKeywords() { return keywords; }
    public User getOwner() { return owner; }
}
