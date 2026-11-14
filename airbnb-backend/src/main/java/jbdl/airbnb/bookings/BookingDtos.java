package jbdl.airbnb.bookings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** What goes in and out of the booking API. */
public final class BookingDtos {

    private BookingDtos() {
    }

    /** POST /api/bookings body: {"listingId":1,"checkIn":"2026-12-20","checkOut":"2026-12-23","guests":2} */
    public record BookingRequest(@NotNull Long listingId, @NotNull LocalDate checkIn, @NotNull LocalDate checkOut,
                                 @Min(1) @Max(16) int guests) {
    }

    /** A booking in responses. */
    public record BookingView(long id, long listingId, String listingTitle, long guestId, LocalDate checkIn,
                              LocalDate checkOut, long nights, int guests, BigDecimal totalPrice, BookingStatus status) {

        public static BookingView of(Booking booking) {
            long nights = new DateRange(booking.getCheckIn(), booking.getCheckOut()).nights();
            return new BookingView(booking.getId(), booking.getListing().getId(), booking.getListing().getTitle(),
                    booking.getGuest().getId(), booking.getCheckIn(), booking.getCheckOut(), nights,
                    booking.getGuests(), booking.getTotalPrice(), booking.getStatus());
        }
    }
}
