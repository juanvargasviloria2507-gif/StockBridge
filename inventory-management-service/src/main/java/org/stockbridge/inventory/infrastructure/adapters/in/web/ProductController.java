package org.stockbridge.inventory.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.stockbridge.inventory.application.port.in.ManageProductUseCase;
import org.stockbridge.inventory.domain.model.Product;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ManageProductUseCase productUseCase;

    public ProductController(ManageProductUseCase productUseCase) {
        this.productUseCase = productUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody ProductBody body) {
        return productUseCase.createProduct(new ManageProductUseCase.CreateProductCommand(
                body.code(), body.name(), body.category(), body.description(), body.price(), body.stock()));
    }

    @GetMapping
    public List<Product> findAll() {
        return productUseCase.getAllProducts();
    }

    @GetMapping("/{id}")
    public Product findById(@PathVariable Long id) {
        return productUseCase.getProductById(id);
    }

    @GetMapping("/code/{code}")
    public Product findByCode(@PathVariable String code) {
        return productUseCase.getProductByCode(code);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @Valid @RequestBody ProductBody body) {
        return productUseCase.updateProduct(id, new ManageProductUseCase.UpdateProductCommand(
                body.code(), body.name(), body.category(), body.description(), body.price(), body.stock()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productUseCase.deleteProduct(id);
    }

    public record ProductBody(@NotBlank(message = "El código del producto es obligatorio.")
                              @Size(max = 80, message = "El código del producto no puede superar los 80 caracteres.") String code,
                              @NotBlank(message = "El nombre del producto es obligatorio.")
                              @Size(max = 160, message = "El nombre del producto no puede superar los 160 caracteres.") String name,
                              @NotBlank(message = "La categoría del producto es obligatoria.")
                              @Size(max = 100, message = "La categoría no puede superar los 100 caracteres.") String category,
                              @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres.") String description,
                              @jakarta.validation.constraints.NotNull(message = "El precio del producto es obligatorio.")
                              @jakarta.validation.constraints.DecimalMin(value = "0.00", message = "El precio no puede ser negativo.") BigDecimal price,
                              @Min(value = 0, message = "El stock no puede ser negativo.") int stock) { }
}
