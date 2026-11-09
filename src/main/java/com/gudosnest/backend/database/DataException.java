package com.gudosnest.backend.database;

/**
 * A database problem, with a message that can be shown to the user as is.
 * The original SQLException is kept as the cause for debugging.
 */
public class DataException extends RuntimeException {
    public DataException(String message, Throwable cause) {
        super(message, cause);
    }
}
