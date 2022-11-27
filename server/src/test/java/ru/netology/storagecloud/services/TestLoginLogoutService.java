package ru.netology.storagecloud.services;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.netology.storagecloud.model.requests.Login;
import ru.netology.storagecloud.model.token.AuthToken;
import ru.netology.storagecloud.repositories.database.entities.TokenEntity;
import ru.netology.storagecloud.repositories.tokens.TokenGenerator;
import ru.netology.storagecloud.repositories.tokens.TokenJpaRepository;
import ru.netology.storagecloud.services.tokens.LoginLogoutService;

import java.util.Optional;

public class TestLoginLogoutService {

    private static long suiteStartTime;
    private long testStartTime;

    @BeforeAll
    public static void initSuite() {
        System.out.println("Running LoginLogoutServiceClassTest");
        suiteStartTime = System.nanoTime();
    }

    @AfterAll
    public static void completeSuite() {
        System.out.println("LoginLogoutServiceClassTest complete: " + (System.nanoTime() - suiteStartTime));
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
    public void checkLoginMethodTest() {
        final var username = "testLogin";
        final var password = "testPassword";
        final var token = "testToken";
        final var authToken = AuthToken.builder().token(token).build();
        final var login = new Login();
        login.setLogin(username);
        login.setPassword(password);
        final var userDetailService = Mockito.mock(UserDetailsService.class);
        final var userDetails = Mockito.mock(UserDetails.class);
        Mockito.when(userDetails.getPassword()).thenReturn(password);
        Mockito.when(userDetails.isCredentialsNonExpired()).thenReturn(true);
        Mockito.when(userDetails.isAccountNonLocked()).thenReturn(true);
        Mockito.when(userDetails.isEnabled()).thenReturn(true);
        Mockito.when(userDetails.isAccountNonExpired()).thenReturn(true);
        Mockito.when(userDetailService.loadUserByUsername(username)).thenReturn(userDetails);
        final var tokenGenerator = Mockito.mock(TokenGenerator.class);
        Mockito.when(tokenGenerator.generateToken(Mockito.any())).thenReturn(authToken);
        final var tokenJpaRepository = Mockito.mock(TokenJpaRepository.class);
        final var passwordEncoder = Mockito.mock(PasswordEncoder.class);
        Mockito.when(passwordEncoder.matches(password, password)).thenReturn(true);
        final var service = new LoginLogoutService(tokenGenerator, passwordEncoder, userDetailService, tokenJpaRepository);
        final var result = service.checkLogin(login);
        Mockito.verify(tokenGenerator, Mockito.times(1)).generateToken(Mockito.any());
        Mockito.verify(tokenJpaRepository, Mockito.times(1)).save(Mockito.any(TokenEntity.class));
        Assertions.assertEquals(result, token);
    }

    @Test
    public void logoutMethodTest() {
        final var request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("auth-token")).thenReturn("auth token");
        final var response = Mockito.mock(HttpServletResponse.class);
        final var tokenGenerator = Mockito.mock(TokenGenerator.class);
        Mockito
                .when(tokenGenerator.readToken(Mockito.anyString()))
                .thenReturn(AuthToken.builder().username("testUsername").build());
        final var passwordEncoder = Mockito.mock(PasswordEncoder.class);
        final var userDetailService = Mockito.mock(UserDetailsService.class);
        final var tokenJpaRepository = Mockito.mock(TokenJpaRepository.class);
        final var tokenEntity = TokenEntity.builder().build();
        Mockito.when(tokenJpaRepository.findById(Mockito.any())).thenReturn(Optional.of(tokenEntity));
        final var service = new LoginLogoutService(tokenGenerator, passwordEncoder, userDetailService, tokenJpaRepository);
        service.logout(request, response);
        Mockito.verify(tokenJpaRepository, Mockito.times(1)).save(tokenEntity);
        final var captor = ArgumentCaptor.forClass(Cookie.class);
        Mockito.verify(response, Mockito.times(1)).addCookie(captor.capture());
        final var argument = captor.getValue();
        Assertions.assertEquals(argument.getName(), "JSESSIONID");
        Assertions.assertNull(argument.getValue());
    }
}
