package jbdl.airbnb.bookings;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import jbdl.airbnb.bookings.BookingDtos.BookingRequest;
import jbdl.airbnb.bookings.BookingDtos.BookingView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The booking API; every call needs a login (SecurityConfig). */
@RestController
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping("/api/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingView book(@Valid @RequestBody BookingRequest request, Principal principal) {
        return service.book(principal.getName(), request);
    }

    /** POST, not DELETE: the booking is not removed, it changes its state (and stays in the history). */
    @PostMapping("/api/bookings/{id}/cancel")
    public BookingView cancel(@PathVariable long id, Principal principal) {
        return service.cancel(id, principal.getName());
    }

    @GetMapping("/api/users/me/bookings")
    public List<BookingView> myBookings(Principal principal) {
        return service.myBookings(principal.getName());
    }

    @GetMapping("/api/listings/{id}/bookings")
    public List<BookingView> listingBookings(@PathVariable long id, Principal principal) {
        return service.listingBookings(id, principal.getName());
    }
}
