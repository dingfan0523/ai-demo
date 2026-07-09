package com.aidemo.exception;

/**
 * Raised when the local AI call concurrency guard rejects a request.
 */
public class AiServiceBusyException extends RuntimeException {

    public AiServiceBusyException(String message) {
        super(message);
    }
}
