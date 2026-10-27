package org.stockbridge.inventory.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockbridge.inventory.infrastructure.adapters.out.persistence.entity.RequestEntity;

public interface SpringDataRequestRepository extends JpaRepository<RequestEntity, Long> { }
