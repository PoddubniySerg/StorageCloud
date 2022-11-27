package ru.netology.storagecloud.model.token;

import lombok.Builder;

@Builder
public record AuthToken(String token, String username, long start, long expiration) {
}
