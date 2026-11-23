package com.spendwise.category;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Picks the best category for a free-text transaction description.
 * A category scores one point per keyword found in the description; longer keyword
 * matches win ties, so "coffee beans" beats "coffee".
 */
@Component
public class CategorySuggester {

    public Optional<Category> suggest(String description, List<Category> categories) {
        if (description == null || description.isBlank()) {
            return Optional.empty();
        }
        String text = description.toLowerCase(Locale.ROOT);
        return categories.stream()
                .map(c -> new Score(c, c.keywordList().stream().filter(text::contains).toList()))
                .filter(s -> !s.matches().isEmpty())
                .max(Comparator.comparingInt((Score s) -> s.matches().size())
                        .thenComparingInt(Score::longestMatch))
                .map(Score::category);
    }

    private record Score(Category category, List<String> matches) {
        int longestMatch() {
            return matches.stream().mapToInt(String::length).max().orElse(0);
        }
    }
}
