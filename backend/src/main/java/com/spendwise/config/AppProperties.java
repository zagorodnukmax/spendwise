package com.spendwise.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, boolean seedDemoData) {

    public record Jwt(String secret, Duration ttl, String issuer) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
