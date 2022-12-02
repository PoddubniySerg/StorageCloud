package ru.netology.storagecloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.RequestEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import ru.netology.storagecloud.config.entities.UserProperties;
import ru.netology.storagecloud.controllers.FileController;
import ru.netology.storagecloud.model.errors.ExceptionResponse;
import ru.netology.storagecloud.model.requests.Login;
import ru.netology.storagecloud.model.responses.AuthTokenResponse;
import ru.netology.storagecloud.services.tokens.TokenDecoder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.testcontainers.junit.jupiter.Testcontainers
class StorageCloudApplicationTests {

    private static final int PORT = 8000;
    private static final String HOST = "http://localhost:" + PORT + "/";
    private static final String LOGIN = "testLogin";
    private static final String PASSWORD = "testPassword";
    private static final String TOKEN_START_WITH = "Bearer ";
    private static final String TOKEN_HEADER_NAME = "auth-token";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres");

    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url=", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username=", POSTGRES::getUsername);
        registry.add("spring.datasource.password=", POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled=", () -> true);
        registry.add("server.port", () -> PORT);
        registry.add("security.users",
                () -> List.of(new UserProperties(LOGIN, PASSWORD, true, List.of("TEST"), "TEST"))
        );
    }

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private TokenDecoder tokenDecoder;
    @Autowired
    private WebApplicationContext webApplicationContext;

    private static long suiteStartTime;
    private long testStartTime;

    @BeforeAll
    public static void initSuite() {
        System.out.println("Running StorageCloudApplicationTests");
        suiteStartTime = System.nanoTime();
        POSTGRES
                .withDatabaseName("postgres")
                .withUsername("test")
                .withPassword("test");
    }

    @AfterAll
    public static void completeSuite() {
        System.out.println("StorageCloudApplicationTests complete: " + (System.nanoTime() - suiteStartTime));
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
    public void loginSuccessTest() throws JsonProcessingException {

        final var url = HOST + "login";
        final var login = new Login();
        login.setLogin(LOGIN);
        login.setPassword(PASSWORD);

        final var request = new RequestEntity<>(login, HttpMethod.POST, URI.create(url));
        final var entity = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        final var responseBody = mapper.readValue(entity.getBody(), AuthTokenResponse.class);
        final var token = tokenDecoder.readToken(responseBody.authToken());
        Assertions.assertEquals(HttpStatusCode.valueOf(200), entity.getStatusCode());
        Assertions.assertNotNull(responseBody.authToken());
        Assertions.assertFalse(responseBody.authToken().isEmpty());
        Assertions.assertFalse(responseBody.authToken().isBlank());
        Assertions.assertNotNull(token);
        Assertions.assertEquals(token.username(), LOGIN);
        Assertions.assertEquals(token.token(), responseBody.authToken());
        Assertions.assertTrue(token.start() > 0 && token.expiration() > 0 && token.expiration() > token.start());
    }

    @ParameterizedTest
    @MethodSource("parametersForLoginInvalidTest")
    public void loginInvalidTest(String username, String password) throws JsonProcessingException {

        final var url = HOST + "login";
        final var login = new Login();
        login.setLogin(username);
        login.setPassword(password);

        final var request = new RequestEntity<>(login, HttpMethod.POST, URI.create(url));
        final var entity = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        final var responseBody = mapper.readValue(entity.getBody(), ExceptionResponse.class);
        Assertions.assertEquals(HttpStatusCode.valueOf(400), entity.getStatusCode());
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(responseBody.id(), 400);
        Assertions.assertNotNull(responseBody.message());
    }

    private static Stream<Arguments> parametersForLoginInvalidTest() {

        return Stream.of(
                Arguments.of(null, null),
                Arguments.of(LOGIN, null),
                Arguments.of(null, PASSWORD),
                Arguments.of("", PASSWORD),
                Arguments.of(LOGIN, " "),
                Arguments.of(LOGIN, "invalidPassword"),
                Arguments.of("invalidLogin", PASSWORD)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForLogoutTest")
    public void logoutTest(String authToken) throws JsonProcessingException {
        var token = (String) null;
        final var urlLogin = HOST + "login";
        switch (authToken) {
            case "null":
                break;
            case "token":
                final var login = new Login();
                login.setLogin(LOGIN);
                login.setPassword(PASSWORD);
                final var requestLogin = new RequestEntity<>(login, HttpMethod.POST, URI.create(urlLogin));
                final var responseLogin = restTemplate.exchange(urlLogin, HttpMethod.POST, requestLogin, String.class);
                token = TOKEN_START_WITH + mapper.readValue(responseLogin.getBody(), AuthTokenResponse.class).authToken();
                break;
            default:
                token = TOKEN_START_WITH + authToken;
                break;
        }

        final var url = HOST + "logout";
        final var headers = token == null ? null : new MultiValueMapAdapter<>(Map.of(TOKEN_HEADER_NAME, List.of(token)));
        final var request = new RequestEntity<>(headers, HttpMethod.POST, URI.create(url));
        final var response = restTemplate.exchange(url, HttpMethod.POST, request, Object.class);

        Assertions.assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
        Assertions.assertNull(response.getBody());
    }

    private static Stream<Arguments> parametersForLogoutTest() {

        return Stream.of(
                Arguments.of("token"),
                Arguments.of("anyString"),
                Arguments.of(""),
                Arguments.of("null"),
                Arguments.of(" ")
        );
    }

    @Test
    void FilesControllerTest() {
        ServletContext servletContext = webApplicationContext.getServletContext();
        Assertions.assertNotNull(servletContext);
        Assertions.assertNotNull(webApplicationContext.getBean(FileController.class));
    }
}