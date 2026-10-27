package org.stockbridge.inventory.infrastructure.adapters.out.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockbridge.inventory.application.port.out.RequestRepositoryPort;
import org.stockbridge.inventory.domain.model.Request;
import org.stockbridge.inventory.infrastructure.adapters.out.persistence.entity.RequestEntity;
import org.stockbridge.inventory.infrastructure.adapters.out.persistence.mapper.RequestMapper;
import org.stockbridge.inventory.infrastructure.adapters.out.persistence.repository.SpringDataRequestRepository;

import java.util.Optional;

@Component
@Transactional
public class RequestPersistenceAdapter implements RequestRepositoryPort {
    private final SpringDataRequestRepository repository;
    private final RequestMapper mapper;

    public RequestPersistenceAdapter(SpringDataRequestRepository repository, RequestMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Request save(Request request) {
        RequestEntity entity = request.id() == null
                ? mapper.toEntity(request)
                : repository.findById(request.id()).map(existing -> {
                    mapper.updateEntity(request, existing);
                    return existing;
                }).orElseGet(() -> mapper.toEntity(request));
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Request> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}
