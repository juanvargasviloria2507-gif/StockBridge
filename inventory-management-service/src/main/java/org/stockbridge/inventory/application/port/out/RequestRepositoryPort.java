package org.stockbridge.inventory.application.port.out;

import org.stockbridge.inventory.domain.model.Request;

import java.util.Optional;

public interface RequestRepositoryPort {
    Request save(Request request);

    Optional<Request> findById(Long id);
}
