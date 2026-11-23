package com.spendwise.category;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CategorySuggesterTest {

    private final CategorySuggester suggester = new CategorySuggester();

    private final Category food = new Category(null, "Food", "#000000", "silpo, coffee");
    private final Category coffeeBeans = new Category(null, "Coffee beans", "#111111", "coffee beans");
    private final Category transport = new Category(null, "Transport", "#222222", "uber, bolt");

    @Test
    void matchesKeywordCaseInsensitive() {
        assertThat(suggester.suggest("UBER ride home", List.of(food, transport))).contains(transport);
    }

    @Test
    void longerKeywordWinsTie() {
        assertThat(suggester.suggest("Bought coffee beans", List.of(food, coffeeBeans))).contains(coffeeBeans);
    }

    @Test
    void returnsEmptyWhenNothingMatches() {
        assertThat(suggester.suggest("Birthday gift", List.of(food, transport))).isEmpty();
        assertThat(suggester.suggest("  ", List.of(food, transport))).isEmpty();
    }
}
