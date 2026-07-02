package com.etiya.cartservice.services.exceptions;

/**
 * Thrown when a business rule is violated (e.g. checking out an empty cart).
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
