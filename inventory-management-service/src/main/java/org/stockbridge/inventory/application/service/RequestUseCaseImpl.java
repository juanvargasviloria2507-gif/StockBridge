package org.stockbridge.inventory.application.service;

import org.stockbridge.inventory.application.port.in.ManageRequestUseCase;
import org.stockbridge.inventory.application.port.out.ProductRepositoryPort;
import org.stockbridge.inventory.application.port.out.RequestRepositoryPort;
import org.stockbridge.inventory.application.port.out.SupplierAvailabilityPort;
import org.stockbridge.inventory.domain.exception.InvalidRequestDataException;
import org.stockbridge.inventory.domain.exception.ProductNotFoundException;
import org.stockbridge.inventory.domain.exception.RequestNotFoundException;
import org.stockbridge.inventory.domain.exception.SupplierIntegrationException;
import org.stockbridge.inventory.domain.exception.UnauthorizedRoleException;
import org.stockbridge.inventory.domain.model.FulfillmentSource;
import org.stockbridge.inventory.domain.model.Product;
import org.stockbridge.inventory.domain.model.Request;
import org.stockbridge.inventory.domain.model.RequestStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public final class RequestUseCaseImpl implements ManageRequestUseCase {
    private final ProductRepositoryPort productRepository;
    private final RequestRepositoryPort requestRepository;
    private final SupplierAvailabilityPort supplierAvailability;
    private final Clock clock;

    public RequestUseCaseImpl(ProductRepositoryPort productRepository,
                              RequestRepositoryPort requestRepository,
                              SupplierAvailabilityPort supplierAvailability,
                              Clock clock) {
        this.productRepository = Objects.requireNonNull(productRepository, "El repositorio de productos es obligatorio.");
        this.requestRepository = Objects.requireNonNull(requestRepository, "El repositorio de solicitudes es obligatorio.");
        this.supplierAvailability = Objects.requireNonNull(supplierAvailability, "El servicio de disponibilidad es obligatorio.");
        this.clock = Objects.requireNonNull(clock, "El reloj del servicio es obligatorio.");
    }

    @Override
    public Request createRequest(CreateRequestCommand command) {
        if (command == null || command.quantity() <= 0 || command.productCode() == null
                || command.productCode().isBlank() || command.requesterEmail() == null
                || command.requesterEmail().isBlank()) {
            throw new InvalidRequestDataException("Se requieren el código del producto, el correo del solicitante y una cantidad positiva.");
        }
        String code = command.productCode().trim().toUpperCase(Locale.ROOT);
        Product product = productRepository.findByCode(code)
                .orElseThrow(() -> new ProductNotFoundException("No se encontró el producto con código " + code + "."));

        Request request;
        if (product.stock() >= command.quantity()) {
            productRepository.save(product.withStock(product.stock() - command.quantity()));
            request = new Request(null, code, command.requesterEmail(), command.quantity(), RequestStatus.PENDIENTE,
                    FulfillmentSource.INTERNAL, Instant.now(clock), null, "Reservada desde el inventario interno.");
        } else {
            SupplierAvailabilityPort.AvailabilityResult availability = supplierAvailability.checkAvailability(
                    code, command.quantity(), product.category());
            if (availability == null) {
                throw new SupplierIntegrationException("El servicio de proveedores no devolvió información de disponibilidad.");
            }
            if (availability.availableQuantity() < 0 || availability.estimatedDeliveryDays() < 0) {
                throw new SupplierIntegrationException("El servicio de proveedores devolvió datos de disponibilidad no válidos.");
            }
            boolean supplierCanFulfill = availability.available()
                    && availability.availableQuantity() >= command.quantity();
            request = new Request(null, code, command.requesterEmail(), command.quantity(),
                    supplierCanFulfill ? RequestStatus.APROBADA : RequestStatus.RECHAZADA,
                    FulfillmentSource.SUPPLIER, Instant.now(clock), availability.estimatedDeliveryDays(),
                    supplierCanFulfill ? "Aprobada según la disponibilidad del proveedor." : "El proveedor no puede cubrir la cantidad solicitada.");
        }
        return requestRepository.save(request);
    }

    @Override
    public Request getRequestById(Long id) {
        return requestRepository.findById(requireId(id))
                .orElseThrow(() -> new RequestNotFoundException("No se encontró la solicitud con identificador " + id + "."));
    }

    @Override
    public Request approveRequest(Long id) {
        Request request = getRequestById(id);
        return requestRepository.save(request.approve());
    }

    @Override
    public Request rejectRequest(Long id, String reason) {
        Request request = getRequestById(id);
        Request rejected = request.reject(reason);
        releaseInternalReservation(request);
        return requestRepository.save(rejected);
    }

    @Override
    public Request deliverRequest(Long id) {
        Request request = getRequestById(id);
        return requestRepository.save(request.deliver());
    }

    @Override
    public Request cancelRequest(Long id, String requesterEmail) {
        Request request = getRequestById(id);
        if (requesterEmail == null || !request.requesterEmail().equals(requesterEmail.trim().toLowerCase(Locale.ROOT))) {
            throw new UnauthorizedRoleException("Solo el solicitante puede cancelar esta solicitud.");
        }
        Request cancelled = request.cancel();
        releaseInternalReservation(request);
        return requestRepository.save(cancelled);
    }

    private void releaseInternalReservation(Request request) {
        if (request.fulfillmentSource() == FulfillmentSource.INTERNAL) {
            Product product = productRepository.findByCode(request.productCode())
                    .orElseThrow(() -> new ProductNotFoundException("No se encontró el producto con código " + request.productCode() + "."));
            productRepository.save(product.withStock(Math.addExact(product.stock(), request.quantity())));
        }
    }

    private static Long requireId(Long id) {
        if (id == null || id <= 0) {
            throw new InvalidRequestDataException("El identificador de la solicitud debe ser positivo.");
        }
        return id;
    }
}
