package ru.netology.storagecloud.services.tokens;

public interface AuthToken {
    String getUsername();
    String getToken();
    long getStart();
    long getExpiration();
}
