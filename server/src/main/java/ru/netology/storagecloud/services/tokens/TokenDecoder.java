package ru.netology.storagecloud.services.tokens;

import ru.netology.storagecloud.model.token.AuthToken;

public interface TokenDecoder {

    AuthToken readToken(String string);
}
