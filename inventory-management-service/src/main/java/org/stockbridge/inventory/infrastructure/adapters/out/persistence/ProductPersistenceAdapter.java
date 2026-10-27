package org.stockbridge.inventory.infrastructure.adapters.out.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockbridge.inventory.application.port.out.ProductRepositoryPort;
import org.stockbridge.inventory.domain.model.Product;
import org.stockbridge.inventory.infrastructure.adapters.out.persistence.entity.ProductEntity;
import org.stockbridge.inventory.infrastructure.adapters.out.persistence.mapper.ProductMapper;
import org.stockbridge.inventory.infrastructure.adapters.out.persistence.repository.SpringDataProductRepository;

import java.util.List;
import java.util.Optional;

@Component
@Transactional
public class ProductPersistenceAdapter implements ProductRepositoryPort {
    private final SpringDataProductRepository repository;
    private final ProductMapper mapper;

    public ProductPersistenceAdapter(SpringDataProductRepository repository, ProductMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Product save(Product product) {
        ProductEntity entity = product.id() == null
                ? mapper.toEntity(product)
                : repository.findById(product.id()).map(existing -> {
                    mapper.updateEntity(product, existing);
                    return existing;
                }).orElseGet(() -> mapper.toEntity(product));
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findByCode(String code) {
        return repository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
