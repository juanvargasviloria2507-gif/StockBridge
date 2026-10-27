package org.stockbridge.inventory.application.service;

import org.stockbridge.inventory.application.port.in.ManageProductUseCase;
import org.stockbridge.inventory.application.port.out.ProductRepositoryPort;
import org.stockbridge.inventory.domain.exception.DuplicateProductException;
import org.stockbridge.inventory.domain.exception.InvalidProductDataException;
import org.stockbridge.inventory.domain.exception.ProductNotFoundException;
import org.stockbridge.inventory.domain.model.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ProductUseCaseImpl implements ManageProductUseCase {
    private final ProductRepositoryPort productRepository;

    public ProductUseCaseImpl(ProductRepositoryPort productRepository) {
        this.productRepository = Objects.requireNonNull(productRepository, "El repositorio de productos es obligatorio.");
    }

    @Override
    public Product createProduct(CreateProductCommand command) {
        if (command == null) {
            throw new InvalidProductDataException("Los datos del producto son obligatorios.");
        }
        String code = normalizeCode(command.code());
        validate(command.name(), command.category(), command.price(), command.stock());
        if (productRepository.findByCode(code).isPresent()) {
            throw new DuplicateProductException("Ya existe un producto con el código " + code + ".");
        }
        return productRepository.save(new Product(null, code, command.name(), command.category(),
                command.description(), command.price(), command.stock()));
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(requireId(id))
                .orElseThrow(() -> new ProductNotFoundException("No se encontró el producto con identificador " + id + "."));
    }

    @Override
    public Product getProductByCode(String code) {
        String normalizedCode = normalizeCode(code);
        return productRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new ProductNotFoundException("No se encontró el producto con código " + normalizedCode + "."));
    }

    @Override
    public List<Product> getAllProducts() {
        return List.copyOf(productRepository.findAll());
    }

    @Override
    public Product updateProduct(Long id, UpdateProductCommand command) {
        Long productId = requireId(id);
        if (command == null) {
            throw new InvalidProductDataException("Los datos del producto son obligatorios.");
        }
        Product current = getProductById(productId);
        String code = normalizeCode(command.code());
        validate(command.name(), command.category(), command.price(), command.stock());
        productRepository.findByCode(code)
                .filter(existing -> !existing.id().equals(productId))
                .ifPresent(existing -> {
                    throw new DuplicateProductException("Ya existe un producto con el código " + code + ".");
                });
        return productRepository.save(new Product(current.id(), code, command.name(), command.category(),
                command.description(), command.price(), command.stock()));
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        productRepository.deleteById(product.id());
    }

    private static void validate(String name, String category, BigDecimal price, int stock) {
        if (name == null || name.isBlank() || category == null || category.isBlank()
                || price == null || price.signum() < 0 || stock < 0) {
            throw new InvalidProductDataException("El nombre, la categoría y un precio válido son obligatorios; el precio y el stock no pueden ser negativos.");
        }
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidProductDataException("El código del producto es obligatorio.");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private static Long requireId(Long id) {
        if (id == null || id <= 0) {
            throw new InvalidProductDataException("El identificador del producto debe ser positivo.");
        }
        return id;
    }
}
