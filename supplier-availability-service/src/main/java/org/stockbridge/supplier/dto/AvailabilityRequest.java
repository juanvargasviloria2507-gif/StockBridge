package org.stockbridge.supplier.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AvailabilityRequest(
        @NotBlank(message = "El código del producto es obligatorio.")
        @Size(max = 80, message = "El código del producto no puede superar los 80 caracteres.") String productCode,
        @Min(value = 1, message = "La cantidad solicitada debe ser al menos 1.") int quantityRequested,
        @NotBlank(message = "La categoría del producto es obligatoria.")
        @Size(max = 100, message = "La categoría no puede superar los 100 caracteres.") String category) { }
