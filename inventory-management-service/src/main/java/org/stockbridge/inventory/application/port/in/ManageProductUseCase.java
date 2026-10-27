package org.stockbridge.inventory.application.port.in;

import org.stockbridge.inventory.domain.model.Product;

import java.math.BigDecimal;
import java.util.List;

public interface ManageProductUseCase {
    Product createProduct(CreateProductCommand command);

    Product getProductById(Long id);

    Product getProductByCode(String code);

    List<Product> getAllProducts();

    Product updateProduct(Long id, UpdateProductCommand command);

    void deleteProduct(Long id);

    record CreateProductCommand(String code, String name, String category, String description,
                                BigDecimal price, int stock) { }

    record UpdateProductCommand(String code, String name, String category, String description,
                                BigDecimal price, int stock) { }
}
