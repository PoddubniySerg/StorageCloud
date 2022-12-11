package ru.netology.storagecloud.services.tokens;

public interface TokenEncoder {

    AuthToken generateToken(String username);
}
