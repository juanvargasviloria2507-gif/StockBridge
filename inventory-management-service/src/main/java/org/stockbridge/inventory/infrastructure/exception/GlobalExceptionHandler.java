package org.stockbridge.inventory.infrastructure.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.stockbridge.inventory.domain.exception.DuplicateProductException;
import org.stockbridge.inventory.domain.exception.EmailAlreadyExistsException;
import org.stockbridge.inventory.domain.exception.InsufficientStockException;
import org.stockbridge.inventory.domain.exception.InvalidCredentialsException;
import org.stockbridge.inventory.domain.exception.InvalidProductDataException;
import org.stockbridge.inventory.domain.exception.InvalidRequestDataException;
import org.stockbridge.inventory.domain.exception.InvalidStatusTransitionException;
import org.stockbridge.inventory.domain.exception.ProductNotFoundException;
import org.stockbridge.inventory.domain.exception.RequestNotFoundException;
import org.stockbridge.inventory.domain.exception.SupplierIntegrationException;
import org.stockbridge.inventory.domain.exception.UnauthorizedRoleException;
import org.stockbridge.inventory.domain.exception.UserNotFoundException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({InvalidProductDataException.class, InvalidRequestDataException.class,
            MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiError> badRequest(Exception exception, HttpServletRequest request) {
        String message;
        if (exception instanceof MethodArgumentNotValidException validation) {
            message = validation.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage).filter(value -> value != null && !value.isBlank())
                    .findFirst().orElse("La solicitud contiene datos no válidos.");
        } else if (exception instanceof HttpMessageNotReadableException) {
            message = "El cuerpo de la solicitud no tiene un formato válido.";
        } else if (exception instanceof ConstraintViolationException) {
            message = "La solicitud contiene datos no válidos.";
        } else if (exception instanceof IllegalArgumentException) {
            message = "La solicitud contiene un argumento no válido.";
        } else {
            message = exception.getMessage();
        }
        return response(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> unauthorized(InvalidCredentialsException exception, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedRoleException.class)
    public ResponseEntity<ApiError> forbidden(UnauthorizedRoleException exception, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler({UserNotFoundException.class, ProductNotFoundException.class, RequestNotFoundException.class,
            NoResourceFoundException.class})
    public ResponseEntity<ApiError> notFound(RuntimeException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler({EmailAlreadyExistsException.class, DuplicateProductException.class,
            InsufficientStockException.class, InvalidStatusTransitionException.class,
            DataIntegrityViolationException.class})
    public ResponseEntity<ApiError> conflict(Exception exception, HttpServletRequest request) {
        String message = exception instanceof DataIntegrityViolationException
                ? "La operación entra en conflicto con datos existentes." : exception.getMessage();
        return response(HttpStatus.CONFLICT, message, request);
    }

    @ExceptionHandler(SupplierIntegrationException.class)
    public ResponseEntity<ApiError> supplierFailure(SupplierIntegrationException exception, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> internalError(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno inesperado.", request);
    }

    private static ResponseEntity<ApiError> response(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), errorLabel(status),
                message == null || message.isBlank() ? errorLabel(status) : message, request.getRequestURI()));
    }

    private static String errorLabel(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Solicitud incorrecta";
            case UNAUTHORIZED -> "No autorizado";
            case FORBIDDEN -> "Acceso denegado";
            case NOT_FOUND -> "No encontrado";
            case CONFLICT -> "Conflicto";
            default -> "Error interno del servidor";
        };
    }

    public record ApiError(Instant timestamp, int status, String error, String message, String path) { }
}
