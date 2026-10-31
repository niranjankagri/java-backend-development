package jbdl.airbnb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import jbdl.airbnb.bookings.Booking;
import jbdl.airbnb.listings.Amenity;
import jbdl.airbnb.listings.Listing;
import jbdl.airbnb.users.AppUser;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Topic 3: the schema Hibernate builds from the entities, and the rules the DATABASE enforces, even
 * if the Java code had a bug. @DataJpaTest starts only JPA (no web layer) and rolls back after
 * each test. flush() sends the SQL now, so the database's answer arrives inside the test; a refused
 * row shows up as Hibernate's ConstraintViolationException (TestEntityManager does not translate it
 * into Spring's DataIntegrityViolationException, as a repository would).
 */
@DataJpaTest
class SchemaDesignTest {

    @Autowired
    private TestEntityManager db;

    private AppUser user(String email) {
        return db.persist(new AppUser("Asha", email, "{noop}not-used-here"));
    }

    private Listing listing(AppUser host, Set<Amenity> amenities) {
        return db.persist(new Listing(host, "Sea view flat", "Two minutes to the beach", "Goa", 4, 2,
                new BigDecimal("3500.00"), amenities));
    }

    @Test
    @DisplayName("app_user.email is UNIQUE: the database refuses a second account with the same email")
    void uniqueEmail() {
        user("asha@example.com");

        assertThatThrownBy(() -> {
            user("asha@example.com");
            db.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("Amenities are rows of their own table listing_amenity, one per amenity")
    void amenitiesTable() {
        Listing listing = listing(user("host@example.com"), Set.of(Amenity.WIFI, Amenity.KITCHEN));
        db.flush();

        List<?> rows = db.getEntityManager()
                .createNativeQuery("select amenity from listing_amenity where listing_id = ? order by amenity")
                .setParameter(1, listing.getId())
                .getResultList();

        // the query returns plain Objects; map(Object::toString) turns each row into its text first
        // (the longer way: a loop that adds row.toString() to a new List<String>)
        assertThat(rows).map(Object::toString).containsExactly("KITCHEN", "WIFI");
    }

    @Test
    @DisplayName("booking has CHECK (check_out > check_in): the database refuses a stay of 0 nights")
    void bookingDatesChecked() {
        AppUser host = user("host2@example.com");
        Listing listing = listing(host, Set.of());
        AppUser guest = user("guest@example.com");
        LocalDate day = LocalDate.of(2026, 12, 20);

        assertThatThrownBy(() -> {
            db.persist(new Booking(listing, guest, day, day, 2, new BigDecimal("0.00")));
            db.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("A valid booking is stored as CONFIRMED with the price agreed")
    void validBooking() {
        Listing listing = listing(user("host3@example.com"), Set.of());
        AppUser guest = user("guest3@example.com");

        Booking booking = db.persistFlushFind(new Booking(listing, guest, LocalDate.of(2026, 12, 20),
                LocalDate.of(2026, 12, 23), 2, new BigDecimal("10500.00")));

        assertThat(booking.getStatus().name()).isEqualTo("CONFIRMED");
        assertThat(booking.getTotalPrice()).isEqualByComparingTo("10500.00");
    }
}
