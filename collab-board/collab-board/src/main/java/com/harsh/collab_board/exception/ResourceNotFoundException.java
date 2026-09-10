package com.harsh.collab_board.exception;

/**
 * Thrown when a requested resource (Board, Card, List, User, etc.)
 * does not exist. Maps to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}