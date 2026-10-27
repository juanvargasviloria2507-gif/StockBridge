package org.stockbridge.inventory.infrastructure.observability;

import io.micrometer.core.instrument.Counter;

public class StockBridgeMetrics {
    private final Counter createdRequests;
    private final Counter supplierAvailabilityChecks;

    public StockBridgeMetrics(Counter createdRequests, Counter supplierAvailabilityChecks) {
        this.createdRequests = createdRequests;
        this.supplierAvailabilityChecks = supplierAvailabilityChecks;
    }

    public void requestCreated() {
        createdRequests.increment();
    }

    public void supplierAvailabilityChecked() {
        supplierAvailabilityChecks.increment();
    }
}
