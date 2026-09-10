package com.harsh.collab_board.exception;

/**
 * Thrown when a request conflicts with existing state
 * (e.g. username already taken, duplicate join request). Maps to HTTP 409.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}