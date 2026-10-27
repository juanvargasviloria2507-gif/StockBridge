package org.stockbridge.inventory.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.stockbridge.inventory.application.port.in.ManageRequestUseCase;
import org.stockbridge.inventory.domain.model.Request;
import org.stockbridge.inventory.infrastructure.observability.StockBridgeMetrics;

import java.security.Principal;

@RestController
@RequestMapping("/api/requests")
public class RequestController {
    private final ManageRequestUseCase requestUseCase;
    private final StockBridgeMetrics metrics;

    public RequestController(ManageRequestUseCase requestUseCase, StockBridgeMetrics metrics) {
        this.requestUseCase = requestUseCase;
        this.metrics = metrics;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Request create(@Valid @RequestBody CreateRequestBody body, Principal principal) {
        Request created = requestUseCase.createRequest(new ManageRequestUseCase.CreateRequestCommand(
                body.productCode(), principal.getName(), body.quantity()));
        metrics.requestCreated();
        return created;
    }

    @GetMapping("/{id}")
    public Request findById(@PathVariable Long id) {
        return requestUseCase.getRequestById(id);
    }

    @PostMapping("/{id}/approve")
    public Request approve(@PathVariable Long id) {
        return requestUseCase.approveRequest(id);
    }

    @PostMapping("/{id}/reject")
    public Request reject(@PathVariable Long id, @RequestBody(required = false) RejectionBody body) {
        return requestUseCase.rejectRequest(id, body == null ? null : body.reason());
    }

    @PostMapping("/{id}/deliver")
    public Request deliver(@PathVariable Long id) {
        return requestUseCase.deliverRequest(id);
    }

    @PostMapping("/{id}/cancel")
    public Request cancel(@PathVariable Long id, Principal principal) {
        return requestUseCase.cancelRequest(id, principal.getName());
    }

    public record CreateRequestBody(@NotBlank(message = "El código del producto es obligatorio.")
                                    @Size(max = 80, message = "El código del producto no puede superar los 80 caracteres.") String productCode,
                                    @Min(value = 1, message = "La cantidad solicitada debe ser al menos 1.") int quantity) { }

    public record RejectionBody(@Size(max = 500, message = "El motivo no puede superar los 500 caracteres.") String reason) { }
}
