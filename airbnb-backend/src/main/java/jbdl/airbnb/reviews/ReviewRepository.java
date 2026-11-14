package jbdl.airbnb.reviews;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByBookingId(long bookingId);

    /** A listing's reviews, the newest first. */
    List<Review> findByListingIdOrderByCreatedAtDescIdDesc(long listingId);

    /** The average rating, computed by the database; null when there are no reviews yet. */
    @Query("select avg(r.rating) from Review r where r.listing.id = :listingId")
    Double averageRating(long listingId);
}
