package org.stockbridge.inventory.infrastructure.adapters.out.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.stockbridge.inventory.application.port.out.SupplierAvailabilityPort;
import org.stockbridge.inventory.domain.exception.SupplierIntegrationException;
import org.stockbridge.inventory.infrastructure.observability.StockBridgeMetrics;

@Component
public class SupplierExternalAdapter implements SupplierAvailabilityPort {
    private final RestClient restClient;
    private final String availabilityUrl;
    private final StockBridgeMetrics metrics;

    public SupplierExternalAdapter(RestClient.Builder builder,
                                   @Value("${supplier.availability.url:http://localhost:8082/api/suppliers/check-availability}") String availabilityUrl,
                                   StockBridgeMetrics metrics) {
        this.restClient = builder.build();
        this.availabilityUrl = availabilityUrl;
        this.metrics = metrics;
    }

    @Override
    public AvailabilityResult checkAvailability(String productCode, int quantityRequested, String category) {
        metrics.supplierAvailabilityChecked();
        try {
            AvailabilityResponse response = restClient.post()
                    .uri(availabilityUrl)
                    .body(new AvailabilityRequest(productCode, quantityRequested, category))
                    .retrieve()
                    .body(AvailabilityResponse.class);
            if (response == null) {
                throw new SupplierIntegrationException("El servicio de proveedores devolvió una respuesta vacía.");
            }
            return new AvailabilityResult(response.available(), response.availableQuantity(), response.estimatedDeliveryDays());
        } catch (RestClientException exception) {
            throw new SupplierIntegrationException("No se pudo consultar la disponibilidad del producto con el servicio de proveedores.");
        }
    }

    public record AvailabilityRequest(String productCode, int quantityRequested, String category) { }

    public record AvailabilityResponse(boolean available, int availableQuantity, int estimatedDeliveryDays) { }
}
