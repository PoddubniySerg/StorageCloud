package ru.netology.storagecloud.security;

import com.nimbusds.jose.KeyLengthException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.netology.storagecloud.exceptions.UnauthorizedException;
import ru.netology.storagecloud.model.errors.ErrorMessage;
import ru.netology.storagecloud.model.token.AuthToken;
import ru.netology.storagecloud.repositories.database.entities.TokenEntity;
import ru.netology.storagecloud.repositories.tokens.TokenGenerator;
import ru.netology.storagecloud.repositories.tokens.TokenJpaRepository;
import ru.netology.storagecloud.services.tokens.TokenDecoder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Stream;

public class TestAuthTokenProvider {

    private static long suiteStartTime;
    private long testStartTime;

    @BeforeAll
    public static void initSuite() {
        System.out.println("Running AuthTokenProviderClassTest");
        suiteStartTime = System.nanoTime();
        final var username = "testUser";
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, ""));
    }

    @AfterAll
    public static void completeSuite() {
        System.out.println("AuthTokenProviderClassTest complete: " + (System.nanoTime() - suiteStartTime));
    }

    @BeforeEach
    public void initTest() {
        System.out.println("Starting new test");
        testStartTime = System.nanoTime();
    }

    @AfterEach
    public void finalizeTest() {
        System.out.println("Test complete: " + (System.nanoTime() - testStartTime));
    }

    @Test
    public void authenticateMethodTest() throws KeyLengthException {
        final var tokenGenerator = new TokenGenerator();
        tokenGenerator.setDaysToExpiration(1);
        final var user = "testUser";
        final var token = tokenGenerator.generateToken(user);
        final var authentication = new UsernamePasswordAuthenticationToken("", token.token());
        final var expected = new UsernamePasswordAuthenticationToken(user, token.token(), new ArrayList<>());

        final var tokenEntity = TokenEntity.builder()
                .username(token.username())
                .token(token.token())
                .isActive(true)
                .start(token.start())
                .expiration(token.expiration())
                .build();

        final var repository = Mockito.mock(TokenJpaRepository.class);
        Mockito.when(repository.findById(token.username())).thenReturn(Optional.of(tokenEntity));
        final var provider = new AuthTokenProvider(repository, tokenGenerator);
        provider.tokenDecoder = Mockito.mock(TokenDecoder.class);
        Mockito.when(provider.tokenDecoder.readToken(Mockito.anyString())).thenReturn(token);
        final var result = provider.authenticate(authentication);
        Assertions.assertEquals(expected, result);
    }

    @ParameterizedTest
    @MethodSource("parametersForAuthenticateMethodWithThrowExceptionTest")
    public void authenticateMethodWithThrowExceptionTest(
            TokenGenerator tokenGenerator,
            Authentication authentication,
            AuthToken token,
            TokenEntity tokenEntity
    ) {
        final Optional<TokenEntity> optionalTokenEntity = tokenEntity == null ? Optional.empty() : Optional.of(tokenEntity);
        final var repository = Mockito.mock(TokenJpaRepository.class);
        Mockito.when(repository.findById(token.username())).thenReturn(optionalTokenEntity);
        final var provider = new AuthTokenProvider(repository, tokenGenerator);
        provider.tokenDecoder = Mockito.mock(TokenDecoder.class);
        Mockito.when(provider.tokenDecoder.readToken(Mockito.anyString())).thenReturn(token);
        Assertions.assertThrows(
                UnauthorizedException.class,
                () -> provider.authenticate(authentication),
                ErrorMessage.UNAUTHORIZED_ERROR
        );
    }

    private static Stream<Arguments> parametersForAuthenticateMethodWithThrowExceptionTest() throws KeyLengthException {

        final var tokenGenerator = new TokenGenerator();
        tokenGenerator.setDaysToExpiration(0);
        final var user = "testUser";
        final var expirationToken = tokenGenerator.generateToken(user);
        tokenGenerator.setDaysToExpiration(1);
        final var token = tokenGenerator.generateToken(user);
        final var authentication = new UsernamePasswordAuthenticationToken("", token.token());


        return Stream.of(

                Arguments.of(tokenGenerator, authentication, token, null),

                Arguments.of(
                        tokenGenerator,
                        authentication,
                        token,
                        TokenEntity.builder()
                                .username(expirationToken.username())
                                .token(expirationToken.token())
                                .isActive(true)
                                .start(expirationToken.start())
                                .expiration(expirationToken.expiration())
                                .build()),
                Arguments.of(
                        tokenGenerator,
                        authentication,
                        token,
                        TokenEntity.builder()
                                .username(token.username())
                                .token(token.token())
                                .isActive(false)
                                .start(token.start())
                                .expiration(token.expiration())
                                .build()),
                Arguments.of(
                        tokenGenerator,
                        authentication,
                        token,
                        TokenEntity.builder()
                                .username(token.username())
                                .token("invalid token")
                                .isActive(true)
                                .start(token.start())
                                .expiration(token.expiration())
                                .build()),
                Arguments.of(
                        tokenGenerator,
                        authentication,
                        token,
                        TokenEntity.builder()
                                .username("invalid username")
                                .token(token.token())
                                .isActive(true)
                                .start(token.start())
                                .expiration(token.expiration())
                                .build()),
                Arguments.of(
                        tokenGenerator,
                        authentication,
                        token,
                        TokenEntity.builder()
                                .username(token.username())
                                .token(token.token())
                                .isActive(true)
                                .start(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                                .expiration(token.expiration())
                                .build()),
                Arguments.of(
                        tokenGenerator,
                        authentication,
                        token,
                        TokenEntity.builder()
                                .username(token.username())
                                .token(token.token())
                                .isActive(true)
                                .start(token.start())
                                .expiration(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                                .build())
        );
    }
}
