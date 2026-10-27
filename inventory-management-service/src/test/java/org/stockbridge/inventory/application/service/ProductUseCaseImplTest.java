package org.stockbridge.inventory.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockbridge.inventory.application.port.in.ManageProductUseCase;
import org.stockbridge.inventory.application.port.out.ProductRepositoryPort;
import org.stockbridge.inventory.domain.exception.DuplicateProductException;
import org.stockbridge.inventory.domain.model.Product;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductUseCaseImplTest {
    @Mock
    private ProductRepositoryPort productRepository;

    @InjectMocks
    private ProductUseCaseImpl productUseCase;

    @Test
    void createsValidProductAndNormalizesCode() {
        when(productRepository.findByCode("SKU-101")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            return new Product(12L, product.code(), product.name(), product.category(), product.description(),
                    product.price(), product.stock());
        });

        Product created = productUseCase.createProduct(new ManageProductUseCase.CreateProductCommand(
                " sku-101 ", "Teclado", "Electrónica", "USB", new BigDecimal("89.90"), 6));

        assertEquals(12L, created.id());
        assertEquals("SKU-101", created.code());
        assertEquals("Teclado", created.name());
        assertEquals(new BigDecimal("89.90"), created.price());
        assertEquals(6, created.stock());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void rejectsDuplicateProductWithoutSaving() {
        when(productRepository.findByCode("SKU-101")).thenReturn(Optional.of(
                new Product(3L, "SKU-101", "Teclado", "Electrónica", "", new BigDecimal("89.90"), 6)));

        DuplicateProductException exception = assertThrows(DuplicateProductException.class,
                () -> productUseCase.createProduct(new ManageProductUseCase.CreateProductCommand(
                        "sku-101", "Otro teclado", "Electrónica", "", new BigDecimal("79.90"), 2)));

        assertEquals("Ya existe un producto con el código SKU-101.", exception.getMessage());
        verify(productRepository, never()).save(any(Product.class));
    }
}
