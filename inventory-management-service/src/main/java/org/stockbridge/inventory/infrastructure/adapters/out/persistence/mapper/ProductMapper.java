package org.stockbridge.inventory.infrastructure.adapters.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.stockbridge.inventory.domain.model.Product;
import org.stockbridge.inventory.infrastructure.adapters.out.persistence.entity.ProductEntity;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(target = "withStock", ignore = true)
    Product toDomain(ProductEntity entity);

    ProductEntity toEntity(Product product);

    void updateEntity(Product product, @MappingTarget ProductEntity entity);
}
