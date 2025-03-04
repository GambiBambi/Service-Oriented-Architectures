package org.bp.microbooking;


import org.bp.microbooking.model.BookVisitRequest;
import org.bp.microbooking.model.BookingInfo;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;

@Service
public class BookingService {
    private HashMap<String, BookingData> booking;

    @PostConstruct
    void init() {
        booking =new HashMap<>();
    }

    public static class BookingData {
        BookVisitRequest bookVisitRequest;
        boolean availability;
        BookingInfo bookingInfo;
        public boolean isReady() {
            return bookVisitRequest!=null && bookingInfo != null && availability;
        }
    }

    public synchronized boolean addBookVisitRequest(String bookVisitId, BookVisitRequest bookVisitRequest) {
        BookingData bookingData = getBookingData(bookVisitId);
        bookingData.bookVisitRequest=bookVisitRequest;
        return bookingData.isReady();
    }

    public synchronized boolean addAvailability(String bookVisitId, boolean availability) {
        BookingData bookingData = getBookingData(bookVisitId);
        bookingData.availability=availability;
        return bookingData.isReady();
    }

    public synchronized boolean addBookingInfo(String bookVisitId, BookingInfo bookingInfo) {
        BookingData bookingData = getBookingData(bookVisitId);
        bookingData.bookingInfo=bookingInfo;
        return bookingData.isReady();
    }


    public synchronized BookingData getBookingData(String bookVisitId) {
        BookingData bookingData = booking.get(bookVisitId);
        if (bookingData==null) {
            bookingData = new BookingData();
            booking.put(bookVisitId, bookingData);
        }
        return bookingData;
    }

    public synchronized boolean removeBooking(String bookVisitId) {
        if (booking.containsKey(bookVisitId)) {
            booking.remove(bookVisitId);
            return true;
        }
        return false;
    }

    public synchronized boolean success(String bookVisitId) {
        BookingData bookingData = booking.get(bookVisitId);
        if (bookingData==null) {
            return false;
        }
        return true;
    }
}
