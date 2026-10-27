package org.stockbridge.supplier.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stockbridge.supplier.dto.AvailabilityRequest;
import org.stockbridge.supplier.dto.AvailabilityResponse;

import java.util.Locale;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {
    @PostMapping("/check-availability")
    public AvailabilityResponse checkAvailability(@Valid @RequestBody AvailabilityRequest request) {
        String category = request.category().trim().toLowerCase(Locale.ROOT);
        int availableQuantity;
        int deliveryDays;
        if (category.equals("discontinued")) {
            availableQuantity = 0;
            deliveryDays = 0;
        } else if (category.equals("food") || category.equals("groceries")) {
            availableQuantity = 100;
            deliveryDays = 2;
        } else if (category.equals("electronics")) {
            availableQuantity = 25;
            deliveryDays = 7;
        } else {
            availableQuantity = 50;
            deliveryDays = 4;
        }
        return new AvailabilityResponse(request.quantityRequested() <= availableQuantity,
                availableQuantity, deliveryDays);
    }
}
