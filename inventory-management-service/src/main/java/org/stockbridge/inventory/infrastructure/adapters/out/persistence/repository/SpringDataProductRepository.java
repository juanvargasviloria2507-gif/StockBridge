package org.stockbridge.inventory.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockbridge.inventory.infrastructure.adapters.out.persistence.entity.ProductEntity;

import java.util.Optional;

public interface SpringDataProductRepository extends JpaRepository<ProductEntity, Long> {
    Optional<ProductEntity> findByCode(String code);
}
