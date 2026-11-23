package com.spendwise.it;

import com.spendwise.support.RealServerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The demo account documented in the README really exists and has data. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:it-demo;DB_CLOSE_DELAY=-1",
        "app.seed-demo-data=true"
})
class DemoAccountIT extends RealServerSupport {

    @Test
    void documentedDemoCredentialsWork() {
        String token = login("demo@spendwise.dev", "demo12345");

        var me = get("/api/me", token);
        assertThat(me.status()).isEqualTo(200);
        assertThat((String) me.json("$.displayName")).isEqualTo("Demo User");
    }

    @Test
    void demoAccountHasSampleDataForTheCurrentMonth() {
        String token = login("demo@spendwise.dev", "demo12345");

        var summary = get("/api/transactions/summary?month=" + YearMonth.now(), token);
        assertThat(summary.status()).isEqualTo(200);
        assertThat(new BigDecimal(summary.json("$.income").toString())).isEqualByComparingTo("45000");
        List<String> categories = summary.json("$.expensesByCategory[*].name");
        assertThat(categories).contains("Groceries", "Uncategorized");

        List<?> defaultCategories = get("/api/categories", token).json("$");
        assertThat(defaultCategories).hasSize(6);
    }

    @Test
    void wrongDemoPasswordIsRejected() {
        var r = json("POST", "/api/auth/login", null, "{\"email\":\"demo@spendwise.dev\",\"password\":\"wrong-pass\"}");
        assertThat(r.status()).isEqualTo(401);
    }
}
