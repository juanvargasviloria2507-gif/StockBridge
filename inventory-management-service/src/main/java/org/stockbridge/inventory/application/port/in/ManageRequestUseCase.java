package org.stockbridge.inventory.application.port.in;

import org.stockbridge.inventory.domain.model.Request;

public interface ManageRequestUseCase {
    Request createRequest(CreateRequestCommand command);

    Request getRequestById(Long id);

    Request approveRequest(Long id);

    Request rejectRequest(Long id, String reason);

    Request deliverRequest(Long id);

    Request cancelRequest(Long id, String requesterEmail);

    record CreateRequestCommand(String productCode, String requesterEmail, int quantity) { }
}
