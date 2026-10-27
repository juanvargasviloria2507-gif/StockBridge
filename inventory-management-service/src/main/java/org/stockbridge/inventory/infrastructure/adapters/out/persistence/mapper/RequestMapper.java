package org.stockbridge.inventory.infrastructure.adapters.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.stockbridge.inventory.domain.model.Request;
import org.stockbridge.inventory.infrastructure.adapters.out.persistence.entity.RequestEntity;

@Mapper(componentModel = "spring")
public interface RequestMapper {
    @Mapping(target = "reject", ignore = true)
    Request toDomain(RequestEntity entity);

    RequestEntity toEntity(Request request);

    void updateEntity(Request request, @MappingTarget RequestEntity entity);
}
