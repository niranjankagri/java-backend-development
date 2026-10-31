package jbdl.airbnb.reviews;

import jakarta.validation.Valid;
import java.security.Principal;
import jbdl.airbnb.reviews.ReviewDtos.ListingReviews;
import jbdl.airbnb.reviews.ReviewDtos.ReviewRequest;
import jbdl.airbnb.reviews.ReviewDtos.ReviewView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Write a review (login) and read a listing's reviews (open to everyone). */
@RestController
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @PostMapping("/api/bookings/{bookingId}/review")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewView write(@PathVariable long bookingId, @Valid @RequestBody ReviewRequest request,
                            Principal principal) {
        return service.write(bookingId, principal.getName(), request);
    }

    @GetMapping("/api/listings/{listingId}/reviews")
    public ListingReviews forListing(@PathVariable long listingId) {
        return service.forListing(listingId);
    }
}
