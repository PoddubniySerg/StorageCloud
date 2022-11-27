package ru.netology.storagecloud;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.netology.storagecloud.model.requests.Login;

import java.net.URI;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class StorageCloudApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final int PORT = 8000;

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres")
            .withDatabaseName("postgres")
            .withUsername("root")
            .withPassword("root");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url=", () -> POSTGRES.getJdbcUrl().split("\\?")[0]);
        registry.add("spring.datasource.username=", POSTGRES::getUsername);
        registry.add("spring.datasource.password=", POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled=", () -> true);
    }

    private static final GenericContainer<?> SERVER = new GenericContainer<>("storage-cloud-server:1.0");

    private static long suiteStartTime;

    private long testStartTime;

    @BeforeAll
    public static void initSuite() {
        System.out.println("Running StorageCloudApplicationTests");
        suiteStartTime = System.nanoTime();
        SERVER.withAccessToHost(true).withExtraHost(POSTGRES.getHost(), "127.0.0.1");
        SERVER.dependsOn(POSTGRES).withExposedPorts(PORT);
//        SERVER.start();
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
    public void contextLoads() {

        SERVER.start();
        final var url = "http://localhost:" + SERVER.getMappedPort(PORT) + "/login";
        final var login = new Login();
        login.setLogin("testLogin");
        login.setPassword("testPassword");
        final var request = new RequestEntity<>(login, HttpMethod.POST, URI.create(url));
        final var entity = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        final var responseBody = entity.getBody();
        Assertions.assertNotNull(responseBody);
        Assertions.assertFalse(responseBody.isEmpty());
        Assertions.assertFalse(responseBody.isBlank());
    }
}
