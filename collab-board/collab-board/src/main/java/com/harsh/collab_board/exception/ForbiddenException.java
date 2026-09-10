package com.harsh.collab_board.exception;

/**
 * Thrown when an authenticated user is not allowed to perform an
 * action (not a board member, not an admin, etc.). Maps to HTTP 403.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}