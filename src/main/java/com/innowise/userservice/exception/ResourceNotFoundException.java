package com.innowise.userservice.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, String field) {
        super(String.format("%s not found with %s", resource, field));
    }
}
