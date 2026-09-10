package com.harsh.collab_board.exception;

/**
 * Thrown for invalid input that isn't covered by Bean Validation
 * (e.g. malformed data, invalid state transitions). Maps to HTTP 400.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}