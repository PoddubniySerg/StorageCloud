package ru.netology.storagecloud.services.tokens;

import ru.netology.storagecloud.model.token.AuthToken;

public interface TokenEncoder {

    AuthToken generateToken(String username);
}
