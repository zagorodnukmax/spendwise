package com.spendwise.auth;

import org.springframework.security.oauth2.jwt.Jwt;

/** Helper to read the authenticated user's id from the validated bearer token. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
