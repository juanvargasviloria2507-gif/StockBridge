package org.stockbridge.inventory.application.port.out;

import org.stockbridge.inventory.domain.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepositoryPort {
    Product save(Product product);

    Optional<Product> findByCode(String code);

    Optional<Product> findById(Long id);

    List<Product> findAll();

    void deleteById(Long id);
}
