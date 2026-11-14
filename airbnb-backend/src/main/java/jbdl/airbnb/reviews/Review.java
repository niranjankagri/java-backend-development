package jbdl.airbnb.reviews;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import jbdl.airbnb.bookings.Booking;
import jbdl.airbnb.listings.Listing;

/**
 * A guest's review of a stay. Tied to the BOOKING, not just the listing: only someone who really stayed
 * can review, and only once per stay (unique booking_id, enforced by the database too).
 */
@Entity
@Table(name = "review",
        indexes = @Index(name = "idx_review_listing", columnList = "listing_id"),
        check = @CheckConstraint(name = "ck_review_rating", constraint = "rating between 1 and 5"))
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // one review per booking: @OneToOne + unique = true
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", unique = true)
    private Booking booking;

    // the listing again, so "all reviews of a listing" needs no join through booking
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @Column(nullable = false)
    private int rating;

    @Column(length = 1000)
    private String comment;

    @Column(nullable = false)
    private Instant createdAt;

    protected Review() {
        // for JPA
    }

    public Review(Booking booking, int rating, String comment) {
        this.booking = booking;
        this.listing = booking.getListing();
        this.rating = rating;
        this.comment = comment;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public Listing getListing() {
        return listing;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
