package org.stockbridge.inventory.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockbridge.inventory.application.port.in.ManageRequestUseCase;
import org.stockbridge.inventory.application.port.out.ProductRepositoryPort;
import org.stockbridge.inventory.application.port.out.RequestRepositoryPort;
import org.stockbridge.inventory.application.port.out.SupplierAvailabilityPort;
import org.stockbridge.inventory.domain.exception.InvalidRequestDataException;
import org.stockbridge.inventory.domain.exception.InvalidStatusTransitionException;
import org.stockbridge.inventory.domain.model.FulfillmentSource;
import org.stockbridge.inventory.domain.model.Product;
import org.stockbridge.inventory.domain.model.Request;
import org.stockbridge.inventory.domain.model.RequestStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestUseCaseImplTest {
    private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");

    @Mock
    private ProductRepositoryPort productRepository;

    @Mock
    private RequestRepositoryPort requestRepository;

    @Mock
    private SupplierAvailabilityPort supplierAvailability;

    private RequestUseCaseImpl requestUseCase;

    @BeforeEach
    void setUp() {
        requestUseCase = new RequestUseCaseImpl(productRepository, requestRepository, supplierAvailability,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void rejectsNonPositiveQuantityBeforeAccessingPorts() {
        InvalidRequestDataException exception = assertThrows(InvalidRequestDataException.class,
                () -> requestUseCase.createRequest(new ManageRequestUseCase.CreateRequestCommand(
                        "SKU-201", "empleado@example.com", 0)));

        assertEquals("Se requieren el código del producto, el correo del solicitante y una cantidad positiva.",
                exception.getMessage());
        verify(productRepository, never()).findByCode(any());
        verify(supplierAvailability, never()).checkAvailability(any(), org.mockito.ArgumentMatchers.anyInt(), any());
    }

    @Test
    void reservesInternalStockForValidRequest() {
        Product product = product(5);
        when(productRepository.findByCode("SKU-201")).thenReturn(Optional.of(product));
        when(requestRepository.save(any(Request.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0), 31L));

        Request created = requestUseCase.createRequest(new ManageRequestUseCase.CreateRequestCommand(
                "sku-201", "empleado@example.com", 3));

        assertEquals(RequestStatus.PENDIENTE, created.status());
        assertEquals(FulfillmentSource.INTERNAL, created.fulfillmentSource());
        assertEquals(NOW, created.createdAt());
        verify(productRepository).save(argThat(saved -> saved.stock() == 2));
        verify(supplierAvailability, never()).checkAvailability(any(), org.mockito.ArgumentMatchers.anyInt(), any());
    }

    @Test
    void approvesRequestWhenMockedSupplierCanFulfillIt() {
        when(productRepository.findByCode("SKU-201")).thenReturn(Optional.of(product(0)));
        when(supplierAvailability.checkAvailability("SKU-201", 4, "Electrónica"))
                .thenReturn(new SupplierAvailabilityPort.AvailabilityResult(true, 9, 3));
        when(requestRepository.save(any(Request.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0), 32L));

        Request created = requestUseCase.createRequest(new ManageRequestUseCase.CreateRequestCommand(
                "SKU-201", "empleado@example.com", 4));

        assertEquals(RequestStatus.APROBADA, created.status());
        assertEquals(FulfillmentSource.SUPPLIER, created.fulfillmentSource());
        assertEquals(3, created.estimatedDeliveryDays());
        assertEquals("Aprobada según la disponibilidad del proveedor.", created.decisionReason());
        verify(supplierAvailability).checkAvailability("SKU-201", 4, "Electrónica");
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void doesNotDeliverRequestRejectedForInsufficientInternalAndSupplierStock() {
        when(productRepository.findByCode("SKU-201")).thenReturn(Optional.of(product(0)));
        when(supplierAvailability.checkAvailability("SKU-201", 4, "Electrónica"))
                .thenReturn(new SupplierAvailabilityPort.AvailabilityResult(false, 1, 5));
        when(requestRepository.save(any(Request.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0), 33L));

        Request rejected = requestUseCase.createRequest(new ManageRequestUseCase.CreateRequestCommand(
                "SKU-201", "empleado@example.com", 4));
        when(requestRepository.findById(33L)).thenReturn(Optional.of(rejected));

        assertEquals(RequestStatus.RECHAZADA, rejected.status());
        assertThrows(InvalidStatusTransitionException.class, () -> requestUseCase.deliverRequest(33L));
        verify(requestRepository, never()).save(argThat(request -> request.status() == RequestStatus.ENTREGADA));
    }

    private static Product product(int stock) {
        return new Product(7L, "SKU-201", "Teclado", "Electrónica", "", new BigDecimal("99.00"), stock);
    }

    private static Request persisted(Request request, Long id) {
        return new Request(id, request.productCode(), request.requesterEmail(), request.quantity(), request.status(),
                request.fulfillmentSource(), request.createdAt(), request.estimatedDeliveryDays(), request.decisionReason());
    }
}
