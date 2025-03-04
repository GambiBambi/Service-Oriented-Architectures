package org.bp.microbooking.exceptions;

public class VisitException extends Exception{
    public VisitException() {
    }

    public VisitException(String message) {
        super(message);
    }

    public VisitException(Throwable cause) {
        super(cause);
    }

    public VisitException(String message, Throwable cause) {
        super(message, cause);
    }

    public VisitException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
