package jbdl.airbnb.listings;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.util.HashSet;
import java.util.Set;
import jbdl.airbnb.users.AppUser;

/**
 * A place a host offers: a flat, a room, a villa. Listings are never deleted, only deactivated:
 * old bookings point to them and must keep making sense (topic 3).
 */
@Entity
// search always filters by city: an index lets the database find a city's listings without reading all rows
@Table(name = "listing", indexes = @Index(name = "idx_listing_city", columnList = "city"))
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // many listings, one host. LAZY: the host row is read only if someone asks for more than its id.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id")
    private AppUser host;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false)
    private int maxGuests;

    @Column(nullable = false)
    private int bedrooms;

    // money: BigDecimal with 2 decimals, never double (Splitwise, Session 12)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal nightlyPrice;

    // A set of simple values, stored in its own table listing_amenity (listing_id, amenity).
    // No entity class is needed for it. EAGER: a listing is always shown with its amenities.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "listing_amenity", joinColumns = @JoinColumn(name = "listing_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "amenity", length = 30)
    private Set<Amenity> amenities = new HashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt;

    protected Listing() {
        // for JPA
    }

    public Listing(AppUser host, String title, String description, String city, int maxGuests, int bedrooms,
                   BigDecimal nightlyPrice, Set<Amenity> amenities) {
        this.host = host;
        this.createdAt = Instant.now();
        setDetails(title, description, city, maxGuests, bedrooms, nightlyPrice, amenities);
    }

    /** Changes everything a host may change. Who the host is and when it was created never change. */
    public void update(String title, String description, String city, int maxGuests, int bedrooms,
                       BigDecimal nightlyPrice, Set<Amenity> amenities) {
        setDetails(title, description, city, maxGuests, bedrooms, nightlyPrice, amenities);
    }

    // private, so the constructor may call it: a constructor that calls a method a subclass could
    // override gives the compiler warning "this-escape" (and warnings fail this course's build)
    private void setDetails(String title, String description, String city, int maxGuests, int bedrooms,
                            BigDecimal nightlyPrice, Set<Amenity> amenities) {
        this.title = title;
        this.description = description;
        this.city = city;
        this.maxGuests = maxGuests;
        this.bedrooms = bedrooms;
        this.nightlyPrice = nightlyPrice;
        this.amenities.clear();
        this.amenities.addAll(amenities);
    }

    /** Hidden from search; existing bookings stay valid. */
    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public AppUser getHost() {
        return host;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCity() {
        return city;
    }

    public int getMaxGuests() {
        return maxGuests;
    }

    public int getBedrooms() {
        return bedrooms;
    }

    public BigDecimal getNightlyPrice() {
        return nightlyPrice;
    }

    public Set<Amenity> getAmenities() {
        return amenities;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
