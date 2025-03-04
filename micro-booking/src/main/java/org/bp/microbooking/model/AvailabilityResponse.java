package org.bp.microbooking.model;

public class AvailabilityResponse {
    private String bookingId;
    private boolean available;
    private BookVisitRequest request;

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public BookVisitRequest getRequest() {
        return request;
    }

    public void setRequest(BookVisitRequest request) {
        this.request = request;
    }
}
