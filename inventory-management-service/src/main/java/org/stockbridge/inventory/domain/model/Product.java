package org.stockbridge.inventory.domain.model;

import org.stockbridge.inventory.domain.exception.InvalidProductDataException;

import java.math.BigDecimal;
import java.util.Locale;

public record Product(Long id, String code, String name, String category, String description,
                      BigDecimal price, int stock) {
    public Product {
        if (code == null || name == null || category == null || price == null) {
            throw new InvalidProductDataException("El código, el nombre, la categoría y el precio del producto son obligatorios.");
        }
        code = code.trim().toUpperCase(Locale.ROOT);
        name = name.trim();
        category = category.trim();
        description = description == null ? "" : description.trim();
        if (name.isBlank() || code.isBlank() || category.isBlank() || price.signum() < 0
                || price.scale() > 2 || price.precision() - price.scale() > 10 || stock < 0) {
            throw new InvalidProductDataException("El código, el nombre y la categoría son obligatorios; el precio debe ser no negativo y tener como máximo dos decimales, y el stock no puede ser negativo.");
        }
    }

    public Product withStock(int newStock) {
        return new Product(id, code, name, category, description, price, newStock);
    }
}
