package org.stockbridge.inventory.domain.model;

import java.time.Instant;
import java.util.Locale;
import org.stockbridge.inventory.domain.exception.InvalidRequestDataException;
import org.stockbridge.inventory.domain.exception.InvalidStatusTransitionException;

public record Request(
        Long id,
        String productCode,
        String requesterEmail,
        int quantity,
        RequestStatus status,
        FulfillmentSource fulfillmentSource,
        Instant createdAt,
        Integer estimatedDeliveryDays,
        String decisionReason) {

    public Request {
        if (productCode == null || productCode.isBlank() || requesterEmail == null || requesterEmail.isBlank()) {
            throw new InvalidRequestDataException("El código del producto y el correo del solicitante son obligatorios.");
        }
        productCode = productCode.trim().toUpperCase(Locale.ROOT);
        requesterEmail = requesterEmail.trim().toLowerCase(Locale.ROOT);
        if (quantity <= 0) {
            throw new InvalidRequestDataException("La cantidad de la solicitud debe ser positiva.");
        }
        if (status == null || fulfillmentSource == null || createdAt == null) {
            throw new InvalidRequestDataException("El estado, el origen de cumplimiento y la fecha de creación son obligatorios.");
        }
        if (estimatedDeliveryDays != null && estimatedDeliveryDays < 0) {
            throw new InvalidRequestDataException("Los días estimados de entrega no pueden ser negativos.");
        }
        decisionReason = decisionReason == null ? "" : decisionReason;
    }

    public Request approve() {
        return transition(RequestStatus.PENDIENTE, RequestStatus.APROBADA, "Aprobada.", estimatedDeliveryDays);
    }

    public Request reject(String reason) {
        return transition(RequestStatus.PENDIENTE, RequestStatus.RECHAZADA,
                reason == null || reason.isBlank() ? "Rechazada." : reason.trim(), null);
    }

    public Request deliver() {
        return transition(RequestStatus.APROBADA, RequestStatus.ENTREGADA, "Entregada.", estimatedDeliveryDays);
    }

    public Request cancel() {
        if (status != RequestStatus.PENDIENTE && status != RequestStatus.APROBADA) {
            throw new InvalidStatusTransitionException("Solo se pueden cancelar las solicitudes pendientes o aprobadas.");
        }
        return new Request(id, productCode, requesterEmail, quantity, RequestStatus.CANCELADA,
                fulfillmentSource, createdAt, null, "Cancelada por el solicitante.");
    }

    private Request transition(RequestStatus expected, RequestStatus target, String reason, Integer deliveryDays) {
        if (status != expected) {
            throw new InvalidStatusTransitionException("No se permite cambiar la solicitud del estado " + status + " al estado " + target + ".");
        }
        return new Request(id, productCode, requesterEmail, quantity, target,
                fulfillmentSource, createdAt, deliveryDays, reason);
    }
}
