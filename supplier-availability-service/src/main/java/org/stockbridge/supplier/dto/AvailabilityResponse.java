package org.stockbridge.supplier.dto;

public record AvailabilityResponse(boolean available, int availableQuantity, int estimatedDeliveryDays) { }
