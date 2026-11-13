package jbdl.airbnb.listings;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** What goes in and out of the listing API. */
public final class ListingDtos {

    private ListingDtos() {
    }

    /**
     * POST /api/listings and PUT /api/listings/{id} body.
     *
     * @param hostId who is asking. Until Session 22 adds login, the caller says who they are; that is
     *               NOT secure, and is replaced by the logged-in user then.
     */
    public record ListingRequest(
            @NotNull Long hostId,
            @NotBlank @Size(max = 120) String title,
            @Size(max = 2000) String description,
            @NotBlank @Size(max = 80) String city,
            @Min(1) @Max(16) int maxGuests,
            @Min(0) @Max(20) int bedrooms,
            @NotNull @DecimalMin("1.00") @Digits(integer = 8, fraction = 2) BigDecimal nightlyPrice,
            Set<Amenity> amenities) {
    }

    /** A listing in responses. Amenities sorted, so the JSON is the same every time. */
    public record ListingView(long id, long hostId, String title, String description, String city,
                              int maxGuests, int bedrooms, BigDecimal nightlyPrice, List<Amenity> amenities,
                              boolean active, Instant createdAt) {

        public static ListingView of(Listing listing) {
            List<Amenity> amenities = new ArrayList<>(listing.getAmenities());
            Collections.sort(amenities);   // enums sort in the order they are declared
            // getHost().getId() does not load the host row: a LAZY reference already knows its id
            return new ListingView(listing.getId(), listing.getHost().getId(), listing.getTitle(),
                    listing.getDescription(), listing.getCity(), listing.getMaxGuests(), listing.getBedrooms(),
                    listing.getNightlyPrice(), amenities, listing.isActive(), listing.getCreatedAt());
        }
    }

    /**
     * One page of search results, with what a client needs to show "page 2 of 5".
     * (Spring's own Page object is not sent as JSON: its shape is not meant to be a stable API.)
     */
    public record SearchPage(List<ListingView> listings, int page, int size, long totalListings, int totalPages) {
    }
}
