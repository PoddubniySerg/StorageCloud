package ru.netology.storagecloud.security;

import lombok.SneakyThrows;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import ru.netology.storagecloud.exceptions.UnauthorizedException;
import ru.netology.storagecloud.model.errors.ErrorMessage;
import ru.netology.storagecloud.repositories.tokens.TokenGenerator;
import ru.netology.storagecloud.repositories.tokens.TokenJpaRepository;
import ru.netology.storagecloud.services.tokens.TokenDecoder;
import ru.netology.storagecloud.services.tokens.TokenEncoder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;

public class AuthTokenProvider implements AuthenticationProvider {

    protected final TokenJpaRepository tokenJpaRepository;
    protected TokenEncoder tokenEncoder;
    protected TokenDecoder tokenDecoder;

    public AuthTokenProvider(TokenJpaRepository tokenJpaRepository, TokenGenerator tokenGenerator) {
        this.tokenJpaRepository = tokenJpaRepository;
        this.tokenEncoder = tokenGenerator;
        this.tokenDecoder = tokenGenerator;
    }

    @SneakyThrows
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        try {
            final var tokenString = authentication.getCredentials().toString();
            final var token = tokenDecoder.readToken(tokenString);
            final var tokenEntity = tokenJpaRepository.findById(token.username()).orElse(null);
            final var nowTime = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            if (
                    tokenEntity == null
                            || !tokenEntity.isActive()
                            || !tokenEntity.getToken().equals(token.token())
                            || !tokenEntity.getUsername().equals(token.username())
                            || tokenEntity.getStart() != token.start()
                            || tokenEntity.getExpiration() != token.expiration()
                            || tokenEntity.getExpiration() < nowTime
            ) {
                throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED_ERROR);
            }
            return new UsernamePasswordAuthenticationToken(tokenEntity.getUsername(), tokenEntity.getToken(), new ArrayList<>());
        } catch (Exception e) {
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED_ERROR);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
