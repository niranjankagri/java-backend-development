package jbdl.airbnb.bookings;

/** Only CONFIRMED bookings block a listing's dates; a CANCELLED one frees them again. */
public enum BookingStatus {
    CONFIRMED,
    CANCELLED
}
