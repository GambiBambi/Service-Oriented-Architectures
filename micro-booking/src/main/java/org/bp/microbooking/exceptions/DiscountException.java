package org.bp.microbooking.exceptions;

public class DiscountException extends Exception{
    public DiscountException() {
    }

    public DiscountException(String message) {
        super(message);
    }

    public DiscountException(Throwable cause) {
        super(cause);
    }

    public DiscountException(String message, Throwable cause) {
        super(message, cause);
    }

    public DiscountException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
