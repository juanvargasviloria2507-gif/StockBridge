package org.stockbridge.inventory.infrastructure.adapters.out.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockbridge.inventory.application.port.out.UserRepositoryPort;
import org.stockbridge.inventory.domain.model.User;
import org.stockbridge.inventory.infrastructure.adapters.out.persistence.entity.UserEntity;
import org.stockbridge.inventory.infrastructure.adapters.out.persistence.mapper.UserMapper;
import org.stockbridge.inventory.infrastructure.adapters.out.persistence.repository.SpringDataUserRepository;

import java.util.Optional;

@Component
@Transactional
public class UserPersistenceAdapter implements UserRepositoryPort {
    private final SpringDataUserRepository repository;
    private final UserMapper mapper;

    public UserPersistenceAdapter(SpringDataUserRepository repository, UserMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        UserEntity entity = user.id() == null
                ? mapper.toEntity(user)
                : repository.findById(user.id()).map(existing -> {
                    mapper.updateEntity(user, existing);
                    return existing;
                }).orElseGet(() -> mapper.toEntity(user));
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }
}
