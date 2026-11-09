package com.gudosnest.backend.service;

/**
 * A business rule was broken (for example "Those dates are already booked").
 * The message is written for the user and is shown in the UI as is.
 */
public class RuleException extends RuntimeException {
    public RuleException(String message) {
        super(message);
    }
}
