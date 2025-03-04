package org.bp.microbooking.exceptions;

public class AvailabilityException extends Exception {
    public AvailabilityException() {
    }

    public AvailabilityException(String message) {
        super(message);
    }

    public AvailabilityException(Throwable cause) {
        super(cause);
    }

    public AvailabilityException(String message, Throwable cause) {
        super(message, cause);
    }

    public AvailabilityException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
