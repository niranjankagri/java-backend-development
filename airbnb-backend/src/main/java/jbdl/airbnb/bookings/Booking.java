package jbdl.airbnb.bookings;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import jbdl.airbnb.listings.Listing;
import jbdl.airbnb.users.AppUser;

/**
 * A guest's stay at a listing. Designed in Session 21 (search must skip booked dates), booked through
 * the API from Session 22.
 *
 * The total price is stored, not recomputed: if the host changes the nightly price later, the
 * booking keeps the price the guest agreed to.
 */
@Entity
@Table(name = "booking",
        // "is this listing free on these dates?" reads bookings by listing and dates
        indexes = @Index(name = "idx_booking_listing_dates", columnList = "listing_id, check_in, check_out"),
        // the database itself refuses a stay that ends before it starts, whatever the code does
        check = @CheckConstraint(name = "ck_booking_dates", constraint = "check_out > check_in"))
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_id")
    private AppUser guest;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(nullable = false)
    private int guests;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected Booking() {
        // for JPA
    }

    public Booking(Listing listing, AppUser guest, LocalDate checkIn, LocalDate checkOut, int guests,
                   BigDecimal totalPrice) {
        this.listing = listing;
        this.guest = guest;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.totalPrice = totalPrice;
        this.status = BookingStatus.CONFIRMED;
        this.createdAt = Instant.now();
    }

    public void cancel() {
        this.status = BookingStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public Listing getListing() {
        return listing;
    }

    public AppUser getGuest() {
        return guest;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public int getGuests() {
        return guests;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
