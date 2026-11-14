package jbdl.airbnb.reviews;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jbdl.airbnb.bookings.Booking;
import jbdl.airbnb.bookings.BookingService;
import jbdl.airbnb.bookings.BookingStatus;
import jbdl.airbnb.common.BusinessRuleException;
import jbdl.airbnb.common.ForbiddenException;
import jbdl.airbnb.listings.ListingService;
import jbdl.airbnb.reviews.ReviewDtos.ListingReviews;
import jbdl.airbnb.reviews.ReviewDtos.ReviewRequest;
import jbdl.airbnb.reviews.ReviewDtos.ReviewView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reviews: only the guest, only after a stay that took place, only once (Session 22, topic 4). */
@Service
public class ReviewService {

    private final ReviewRepository reviews;
    private final BookingService bookings;
    private final ListingService listings;

    public ReviewService(ReviewRepository reviews, BookingService bookings, ListingService listings) {
        this.reviews = reviews;
        this.bookings = bookings;
        this.listings = listings;
    }

    @Transactional
    public ReviewView write(long bookingId, String guestEmail, ReviewRequest request) {
        Booking booking = bookings.getEntity(bookingId);                       // 404
        if (!booking.getGuest().getEmail().equals(guestEmail)) {
            throw new ForbiddenException("only the guest of booking " + bookingId + " can review it");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessRuleException("a cancelled booking cannot be reviewed");
        }
        if (booking.getCheckOut().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("a stay can be reviewed from its check-out day");
        }
        if (reviews.existsByBookingId(bookingId)) {
            throw new BusinessRuleException("booking " + bookingId + " already has a review");
        }
        return ReviewView.of(reviews.save(new Review(booking, request.rating(), request.comment())));
    }

    @Transactional(readOnly = true)
    public ListingReviews forListing(long listingId) {
        listings.getEntity(listingId);                                         // 404 for an unknown listing
        List<ReviewView> views = new ArrayList<>();
        for (Review review : reviews.findByListingIdOrderByCreatedAtDescIdDesc(listingId)) {
            views.add(ReviewView.of(review));
        }
        Double average = reviews.averageRating(listingId);
        // one decimal, rounded half up: 4.666... -> 4.7
        BigDecimal rounded = (average == null) ? null : BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP);
        return new ListingReviews(listingId, rounded, views.size(), views);
    }
}
