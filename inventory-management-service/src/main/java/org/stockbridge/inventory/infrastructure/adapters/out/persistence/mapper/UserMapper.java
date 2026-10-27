package org.stockbridge.inventory.infrastructure.adapters.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.stockbridge.inventory.domain.model.User;
import org.stockbridge.inventory.infrastructure.adapters.out.persistence.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toDomain(UserEntity entity);

    UserEntity toEntity(User user);

    void updateEntity(User user, @MappingTarget UserEntity entity);
}
