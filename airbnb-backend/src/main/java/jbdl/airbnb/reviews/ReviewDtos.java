package jbdl.airbnb.reviews;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** What goes in and out of the review API. */
public final class ReviewDtos {

    private ReviewDtos() {
    }

    /** POST /api/bookings/{id}/review body: {"rating":5,"comment":"Lovely view"} */
    public record ReviewRequest(@Min(1) @Max(5) int rating, @Size(max = 1000) String comment) {
    }

    /** One review. The guest's first name only, as on Airbnb; never their email. */
    public record ReviewView(long id, long bookingId, String guestName, int rating, String comment, Instant createdAt) {

        public static ReviewView of(Review review) {
            return new ReviewView(review.getId(), review.getBooking().getId(),
                    review.getBooking().getGuest().getName(), review.getRating(), review.getComment(),
                    review.getCreatedAt());
        }
    }

    /**
     * GET /api/listings/{id}/reviews answer.
     *
     * @param averageRating with one decimal (4.5), or null without reviews
     */
    public record ListingReviews(long listingId, BigDecimal averageRating, int reviewCount, List<ReviewView> reviews) {
    }
}
