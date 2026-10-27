package org.stockbridge.supplier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.stockbridge.supplier.controller.SupplierController;
import org.stockbridge.supplier.exception.GlobalExceptionHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SupplierController.class)
@Import(GlobalExceptionHandler.class)
class SupplierControllerWebTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsDeterministicAvailabilityForFoodCategory() throws Exception {
        mockMvc.perform(post("/api/suppliers/check-availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"SKU-1\",\"quantityRequested\":5,\"category\":\"food\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.availableQuantity").value(100))
                .andExpect(jsonPath("$.estimatedDeliveryDays").value(2));
    }

    @Test
    void rejectsInvalidQuantityWithSpanishMessage() throws Exception {
        mockMvc.perform(post("/api/suppliers/check-availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"SKU-1\",\"quantityRequested\":0,\"category\":\"food\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La cantidad solicitada debe ser al menos 1."));
    }
}
