package com.gerimedica.csvservice.exception;

public class NonUniqueCodeException extends RuntimeException {
    public NonUniqueCodeException(String message) {
        super(message);
    }
}
