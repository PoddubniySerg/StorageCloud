package ru.netology.storagecloud.repositories.tokens;

import lombok.Builder;
import ru.netology.storagecloud.services.tokens.AuthToken;

@Builder
public record AuthTokenGenerated(String token, String username, long start, long expiration) implements AuthToken {
    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public long getStart() {
        return start;
    }

    @Override
    public long getExpiration() {
        return expiration;
    }
}
