package ru.netology.storagecloud.services.tokens;

public interface TokenDecoder {

    AuthToken readToken(String string);
}
