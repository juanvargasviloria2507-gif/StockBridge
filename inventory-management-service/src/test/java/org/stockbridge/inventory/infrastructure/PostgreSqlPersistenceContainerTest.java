package org.stockbridge.inventory.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.stockbridge.inventory.application.port.out.ProductRepositoryPort;
import org.stockbridge.inventory.application.port.out.RequestRepositoryPort;
import org.stockbridge.inventory.application.port.out.UserRepositoryPort;
import org.stockbridge.inventory.domain.model.FulfillmentSource;
import org.stockbridge.inventory.domain.model.Product;
import org.stockbridge.inventory.domain.model.Request;
import org.stockbridge.inventory.domain.model.RequestStatus;
import org.stockbridge.inventory.domain.model.Role;
import org.stockbridge.inventory.domain.model.User;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlPersistenceContainerTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("stockbridge_test")
            .withUsername("stockbridge_test")
            .withPassword("stockbridge_test");

    @Autowired
    private ProductRepositoryPort productRepository;

    @Autowired
    private UserRepositoryPort userRepository;

    @Autowired
    private RequestRepositoryPort requestRepository;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Test
    void flywaySchemaAndAdaptersPersistProductUserAndRequestInPostgres() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String productCode = "TC-" + suffix.toUpperCase();
        String email = "test-" + suffix + "@example.com";

        User savedUser = userRepository.save(new User(null, "Usuario Testcontainers", email, "hash-de-prueba",
                Role.ROLE_EMPLEADO));
        Product savedProduct = productRepository.save(new Product(null, productCode, "Producto Testcontainers",
                "Electrónica", "", new BigDecimal("25.50"), 7));
        Request savedRequest = requestRepository.save(new Request(null, productCode, email, 2,
                RequestStatus.PENDIENTE, FulfillmentSource.INTERNAL, Instant.parse("2026-01-15T12:00:00Z"),
                null, "Reserva de prueba"));

        assertNotNull(savedUser.id());
        assertNotNull(savedProduct.id());
        assertNotNull(savedRequest.id());
        assertTrue(POSTGRES.isRunning());
        assertEquals("Producto Testcontainers", productRepository.findByCode(productCode).orElseThrow().name());
        assertEquals(Role.ROLE_EMPLEADO, userRepository.findByEmail(email).orElseThrow().role());
        assertEquals(RequestStatus.PENDIENTE,
                requestRepository.findById(savedRequest.id()).orElseThrow().status());
    }
}
