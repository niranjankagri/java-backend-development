package jbdl.airbnb.reviews;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByBookingId(long bookingId);

    /**
     * A listing's reviews, the newest first. The entity graph fetches each review's booking and its guest
     * in the same query (joins), because ReviewView needs the guest's name. Without it, every review
     * would load its booking and then its guest lazily, one extra query each: 1 + 2N queries (N+1).
     */
    @EntityGraph(attributePaths = {"booking", "booking.guest"})
    List<Review> findByListingIdOrderByCreatedAtDescIdDesc(long listingId);

    /** The average rating, computed by the database; null when there are no reviews yet. */
    @Query("select avg(r.rating) from Review r where r.listing.id = :listingId")
    Double averageRating(long listingId);
}
