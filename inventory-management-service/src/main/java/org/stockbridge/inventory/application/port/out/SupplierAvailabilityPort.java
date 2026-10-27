package org.stockbridge.inventory.application.port.out;

public interface SupplierAvailabilityPort {
    AvailabilityResult checkAvailability(String productCode, int quantityRequested, String category);

    record AvailabilityResult(boolean available, int availableQuantity, int estimatedDeliveryDays) { }
}
