package jbdl.airbnb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import jbdl.airbnb.bookings.BookingDtos.BookingRequest;
import jbdl.airbnb.bookings.BookingService;
import jbdl.airbnb.common.BusinessRuleException;
import jbdl.airbnb.common.TimeConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Session 23: the application with the PROD profile against a real PostgreSQL (Testcontainers), the way
 * it runs in its container. Flyway creates the schema, Hibernate only validates it. If the migrations
 * and the entities did not match, the application context would not even start.
 * Without Docker the class is skipped, not failed (CI has Docker).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@Testcontainers(disabledWithoutDocker = true)
class ProdProfileTest extends ApiTestSupport {

    // @ServiceConnection hands the container's URL, user and password to Spring Boot; they win over
    // the DATABASE_URL defaults in application-prod.properties
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    private static final LocalDate CHECK_IN = LocalDate.now(TimeConfig.BUSINESS_ZONE).plusDays(30);
    private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(3);

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private BookingService bookingService;

    @Test
    @DisplayName("Flyway ran all three migrations on PostgreSQL, and Hibernate's validation passed")
    void migrations() {
        List<String> applied = jdbc.queryForList(
                "select version || ' ' || description from flyway_schema_history where success order by installed_rank",
                String.class);

        assertThat(applied).containsExactly("1 create schema", "2 no overlapping bookings", "3 index lower city");
        assertThat(jdbc.queryForObject("select version()", String.class)).startsWith("PostgreSQL 18");
    }

    @Test
    @DisplayName("The city search can use the index on lower(city)")
    void citySearchUsesTheIndex() {
        // With a handful of rows PostgreSQL would rightly read the whole table anyway, so sequential scans
        // are switched off for this one check: the question is whether the index CAN serve the query.
        String plan = jdbc.execute((ConnectionCallback<String>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("set enable_seqscan = off");
                StringBuilder lines = new StringBuilder();
                try (ResultSet rows = statement.executeQuery(
                        "explain select * from listing where lower(city) = lower('Goa')")) {
                    while (rows.next()) {
                        lines.append(rows.getString(1)).append('\n');
                    }
                }
                statement.execute("reset enable_seqscan");
                return lines.toString();
            }
        });
        assertThat(plan).contains("idx_listing_lower_city");
    }

    @Test
    @DisplayName("Health without a login: UP with the database; liveness and readiness UP")
    void health() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"));
        mvc.perform(get("/actuator/health/liveness")).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/health/readiness")).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("The main flow on PostgreSQL: listing, booking, the same nights again 409, search without them")
    void mainFlow() throws Exception {
        TestUser asha = registerUser("Asha");
        TestUser ravi = registerUser("Ravi");
        TestUser meera = registerUser("Meera");
        String city = uniqueCity("Goa");
        long flat = createListing(asha, "Sea view flat", city, 4, "3500.00");

        postJson("/api/bookings", bookingJson(flat, CHECK_IN, CHECK_OUT, 2), ravi)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalPrice").value(10500.00));
        postJson("/api/bookings", bookingJson(flat, CHECK_IN, CHECK_OUT, 2), meera)
                .andExpect(status().isConflict());

        mvc.perform(get("/api/listings/search?city=" + city.toLowerCase() + "&checkIn=" + CHECK_IN + "&checkOut=" + CHECK_OUT))
                .andExpect(jsonPath("$.totalListings").value(0));
    }

    @Test
    @DisplayName("V2's exclusion constraint: the DATABASE refuses an overlapping confirmed booking, even past the code")
    void exclusionConstraint() throws Exception {
        TestUser asha = registerUser("Asha");
        TestUser ravi = registerUser("Ravi");
        long flat = createListing(asha, "Sea view flat", uniqueCity("Goa"), 4, "3500.00");

        // plain SQL, around BookingService and its lock, as a script or another service could write
        insertBooking(flat, ravi.id(), CHECK_IN, CHECK_OUT, "CONFIRMED");

        assertThatThrownBy(() -> insertBooking(flat, ravi.id(), CHECK_IN.plusDays(1), CHECK_OUT.plusDays(1), "CONFIRMED"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ex_booking_no_overlap");

        // allowed: back to back (the check-out day is free), and an overlapping CANCELLED booking
        insertBooking(flat, ravi.id(), CHECK_OUT, CHECK_OUT.plusDays(2), "CONFIRMED");
        insertBooking(flat, ravi.id(), CHECK_IN, CHECK_OUT, "CANCELLED");
    }

    @Test
    @DisplayName("10 guests book the same nights at once on PostgreSQL: exactly 1 booking (SELECT ... FOR UPDATE)")
    void raceOnPostgres() throws Exception {
        TestUser asha = registerUser("Asha");
        long flat = createListing(asha, "Sea view flat", uniqueCity("Goa"), 4, "3500.00");
        List<String> guests = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            guests.add(registerUser("Guest" + i).email());
        }
        BookingRequest sameNights = new BookingRequest(flat, CHECK_IN, CHECK_OUT, 2);

        CountDownLatch startLine = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(guests.size());
        List<Future<Boolean>> results = new ArrayList<>();
        for (String guest : guests) {
            Callable<Boolean> attempt = () -> {
                startLine.await();
                try {
                    bookingService.book(guest, sameNights);
                    return true;
                } catch (BusinessRuleException e) {
                    return false;
                }
            };
            results.add(pool.submit(attempt));
        }
        startLine.countDown();

        int booked = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                booked++;
            }
        }
        pool.shutdown();
        assertThat(booked).isEqualTo(1);
    }

    private void insertBooking(long listingId, long guestId, LocalDate checkIn, LocalDate checkOut, String status) {
        jdbc.update("""
                insert into booking (listing_id, guest_id, check_in, check_out, guests, total_price, status, created_at)
                values (?, ?, ?, ?, 2, 1000.00, ?, now())
                """, listingId, guestId, checkIn, checkOut, status);
    }
}
