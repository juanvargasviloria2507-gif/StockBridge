package org.stockbridge.inventory.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.stockbridge.inventory.domain.model.Role;
import org.stockbridge.inventory.infrastructure.adapters.in.web.AuthController;
import org.stockbridge.inventory.infrastructure.adapters.in.web.ProductController;
import org.stockbridge.inventory.infrastructure.security.JwtProvider;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryManagementWebIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void registrationLoginAndDuplicateRegistrationUsePersistedCredentials() throws Exception {
        String email = uniqueEmail();
        AuthController.AuthResponse registered = register(email);

        assertNotNull(registered.token());
        assertEquals(Role.ROLE_EMPLEADO.name(), registered.role());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthController.LoginRequest(email, "ClaveSegura123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthController.RegisterRequest("Empleado", email, "ClaveSegura123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe una cuenta registrada con este correo."));
    }

    @Test
    void employeeCanReadProductsButCannotCreateProductsAndUnauthenticatedRequestIsRejected() throws Exception {
        AuthController.AuthResponse employee = register(uniqueEmail());
        String productJson = productJson(uniqueCode(), 8);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Se requiere autenticación para acceder a este recurso."));

        mockMvc.perform(get("/api/products").header("Authorization", bearer(employee.token())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/products")
                        .header("Authorization", bearer(employee.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado"));
    }

    @Test
    void warehouseCreatesPersistentProductAndCompletesInternalRequestFlow() throws Exception {
        String code = uniqueCode();
        String warehouseToken = token("bodega@example.com", Role.ROLE_BODEGA);
        AuthController.AuthResponse employee = register(uniqueEmail());

        MvcResult createdProduct = mockMvc.perform(post("/api/products")
                        .header("Authorization", bearer(warehouseToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(code, 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code))
                .andReturn();
        JsonNode product = objectMapper.readTree(createdProduct.getResponse().getContentAsString());
        long productId = product.get("id").asLong();

        mockMvc.perform(get("/api/products/{id}", productId)
                        .header("Authorization", bearer(employee.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(5));

        MvcResult createdRequest = mockMvc.perform(post("/api/requests")
                        .header("Authorization", bearer(employee.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"" + code + "\",\"quantity\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDIENTE"))
                .andExpect(jsonPath("$.fulfillmentSource").value("INTERNAL"))
                .andReturn();
        long requestId = objectMapper.readTree(createdRequest.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/products/code/{code}", code)
                        .header("Authorization", bearer(employee.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(2));

        mockMvc.perform(post("/api/requests/{id}/approve", requestId)
                        .header("Authorization", bearer(employee.token())))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/requests/{id}/approve", requestId)
                        .header("Authorization", bearer(warehouseToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROBADA"));

        mockMvc.perform(post("/api/requests/{id}/deliver", requestId)
                        .header("Authorization", bearer(warehouseToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGADA"));

        mockMvc.perform(get("/api/requests/{id}", requestId)
                        .header("Authorization", bearer(employee.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGADA"))
                .andExpect(jsonPath("$.requesterEmail").value(employee.email()));
    }

    @Test
    void invalidRequestQuantityReturnsSpanishValidationResponse() throws Exception {
        AuthController.AuthResponse employee = register(uniqueEmail());

        mockMvc.perform(post("/api/requests")
                        .header("Authorization", bearer(employee.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"SKU-INVALID\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La cantidad solicitada debe ser al menos 1."));
    }

    private AuthController.AuthResponse register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthController.RegisterRequest("Empleado de prueba", email, "ClaveSegura123"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthController.AuthResponse.class);
    }

    private String productJson(String code, int stock) throws Exception {
        ProductController.ProductBody body = new ProductController.ProductBody(code, "Producto de prueba", "Alimentos",
                "", new BigDecimal("12.50"), stock);
        return objectMapper.writeValueAsString(body);
    }

    private String token(String email, Role role) {
        return jwtProvider.generateToken(email, role);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String uniqueEmail() {
        return "empleado-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    private static String uniqueCode() {
        return "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
