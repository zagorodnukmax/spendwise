package com.spendwise.it;

import com.spendwise.support.RealServerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A whole user session over real HTTP: register → categories → transactions → summary → cleanup. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:it-flow;DB_CLOSE_DELAY=-1",
        "app.seed-demo-data=false"
})
class ExpenseFlowIT extends RealServerSupport {

    @Test
    void fullUserJourney() {
        String token = registerUser();

        // 1. Own category with keywords
        var pets = json("POST", "/api/categories", token,
                "{\"name\":\"Pets\",\"color\":\"#f59e0b\",\"keywords\":\"vet, pet food\"}");
        assertThat(pets.status()).isEqualTo(201);

        // 2. Transactions without a category are auto-categorized
        var food = json("POST", "/api/transactions", token,
                "{\"amount\":300,\"type\":\"EXPENSE\",\"description\":\"Pet food for Murchyk\",\"date\":\"2026-11-10\"}");
        assertThat(food.status()).isEqualTo(201);
        assertThat((String) food.json("$.category.name")).isEqualTo("Pets");

        var courier = json("POST", "/api/transactions", token,
                "{\"amount\":120.50,\"type\":\"EXPENSE\",\"description\":\"Courier service\",\"date\":\"2026-11-11\"}");
        assertThat((String) courier.json("$.category")).isNull(); // no default keyword matches

        json("POST", "/api/transactions", token,
                "{\"amount\":30000,\"type\":\"INCOME\",\"description\":\"Salary\",\"date\":\"2026-11-01\"}");

        // 3. Fix the category by hand by editing the transaction
        int courierId = courier.json("$.id");
        List<Integer> transportIds = get("/api/categories", token).json("$[?(@.name == 'Transport')].id");
        var fixed = json("PUT", "/api/transactions/" + courierId, token,
                "{\"amount\":120.50,\"type\":\"EXPENSE\",\"description\":\"Courier service\",\"date\":\"2026-11-11\",\"categoryId\":%d}"
                        .formatted(transportIds.getFirst()));
        assertThat((String) fixed.json("$.category.name")).isEqualTo("Transport");

        // 4. Monthly summary reflects everything
        var summary = get("/api/transactions/summary?month=2026-11", token);
        assertThat(summary.body()).contains("\"income\":30000", "\"expenses\":420.50", "\"balance\":29579.50");
        List<String> names = summary.json("$.expensesByCategory[*].name");
        assertThat(names).containsExactly("Pets", "Transport");

        // 5. List is sorted newest first, delete works
        List<String> descriptions = get("/api/transactions?month=2026-11", token).json("$[*].description");
        assertThat(descriptions).containsExactly("Courier service", "Pet food for Murchyk", "Salary");
        assertThat(delete("/api/transactions/" + courierId, token).status()).isEqualTo(204);
        assertThat(get("/api/transactions?month=2026-11", token).body()).doesNotContain("Courier");
    }

    @Test
    void dataIsIsolatedBetweenUsers() {
        String alice = registerUser();
        String bob = registerUser();
        int id = json("POST", "/api/transactions", alice,
                "{\"amount\":50,\"type\":\"EXPENSE\",\"description\":\"Coffee\",\"date\":\"2026-11-03\"}").json("$.id");

        assertThat(get("/api/transactions?month=2026-11", bob).body()).isEqualTo("[]");
        assertThat(delete("/api/transactions/" + id, bob).status()).isEqualTo(404);
        assertThat(get("/api/transactions?month=2026-11", alice).body()).contains("Coffee");
    }
}
