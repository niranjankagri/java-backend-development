package jbdl.airbnb;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import jbdl.airbnb.bookings.Booking;
import jbdl.airbnb.bookings.BookingRepository;
import jbdl.airbnb.common.TimeConfig;
import jbdl.airbnb.listings.ListingRepository;
import jbdl.airbnb.reviews.Review;
import jbdl.airbnb.reviews.ReviewRepository;
import jbdl.airbnb.reviews.ReviewService;
import jbdl.airbnb.users.AppUserRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

/**
 * The N+1 check for a listing's reviews: Hibernate's statistics count the SQL statements of one call to
 * ReviewService.forListing. If each review loaded its booking and guest lazily, three reviews would take
 * four statements more than one review; with the entity graph the count does not depend on the number
 * of reviews.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
class ReviewQueryCountTest extends ApiTestSupport {

    @Autowired
    private ReviewService reviewService;
    @Autowired
    private ReviewRepository reviews;
    @Autowired
    private BookingRepository bookings;
    @Autowired
    private ListingRepository listings;
    @Autowired
    private AppUserRepository users;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    /** A listing with the given number of reviews, each by a different guest after a past stay. */
    private long listingWithReviews(int count) throws Exception {
        TestUser host = registerUser("Asha");
        long flat = createListing(host, "Sea view flat", uniqueCity("Goa"), 4, "3500.00");
        for (int i = 0; i < count; i++) {
            TestUser guest = registerUser("Guest");
            Booking stay = bookings.save(new Booking(listings.getReferenceById(flat), users.getReferenceById(guest.id()),
                    LocalDate.now(TimeConfig.BUSINESS_ZONE).minusDays(10), LocalDate.now(TimeConfig.BUSINESS_ZONE).minusDays(8), 2, new BigDecimal("7000.00")));
            reviews.save(new Review(stay, 5, "Lovely"));
        }
        return flat;
    }

    /** The number of SQL statements one call to forListing sends. */
    private long statementsFor(long listingId) {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(reviewService.forListing(listingId).reviews()).isNotEmpty();
        return statistics.getPrepareStatementCount();
    }

    @Test
    @DisplayName("A listing's reviews: the same number of SQL statements for 1 review and for 3")
    void noQueryPerReview() throws Exception {
        long one = statementsFor(listingWithReviews(1));
        long three = statementsFor(listingWithReviews(3));

        assertThat(three).isEqualTo(one);
    }
}
