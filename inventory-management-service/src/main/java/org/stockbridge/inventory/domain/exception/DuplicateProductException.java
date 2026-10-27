package org.stockbridge.inventory.domain.exception;

public class DuplicateProductException extends RuntimeException {
    public DuplicateProductException(String message) { super(message); }
}
