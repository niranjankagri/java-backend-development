package jbdl.airbnb.listings;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jbdl.airbnb.bookings.DateRange;
import jbdl.airbnb.common.ForbiddenException;
import jbdl.airbnb.common.NotFoundException;
import jbdl.airbnb.listings.ListingDtos.ListingRequest;
import jbdl.airbnb.listings.ListingDtos.ListingView;
import jbdl.airbnb.listings.ListingDtos.SearchPage;
import jbdl.airbnb.users.AppUser;
import jbdl.airbnb.users.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Hosts manage their listings; guests search them. */
@Service
public class ListingService {

    /**
     * "No upper price limit": the largest price the column can hold (10 digits, 2 decimals). Simpler
     * than writing two more queries for searches without a budget.
     */
    static final BigDecimal NO_PRICE_LIMIT = new BigDecimal("99999999.99");

    private final ListingRepository listings;
    private final UserService users;

    public ListingService(ListingRepository listings, UserService users) {
        this.listings = listings;
        this.users = users;
    }

    @Transactional
    public ListingView create(ListingRequest request) {
        AppUser host = users.get(request.hostId());   // 404 if the host does not exist
        Listing listing = new Listing(host, request.title().trim(), request.description(), request.city().trim(),
                request.maxGuests(), request.bedrooms(), request.nightlyPrice(), amenities(request));
        return ListingView.of(listings.save(listing));
    }

    @Transactional(readOnly = true)
    public ListingView get(long id) {
        return ListingView.of(find(id));
    }

    @Transactional
    public ListingView update(long id, ListingRequest request) {
        Listing listing = findOwnedBy(id, request.hostId());
        listing.update(request.title().trim(), request.description(), request.city().trim(), request.maxGuests(),
                request.bedrooms(), request.nightlyPrice(), amenities(request));
        // no save() needed: the entity is managed, JPA writes the changes when the transaction ends
        return ListingView.of(listing);
    }

    @Transactional
    public void deactivate(long id, long hostId) {
        findOwnedBy(id, hostId).deactivate();
    }

    @Transactional(readOnly = true)
    public List<ListingView> listingsOf(long hostId) {
        users.get(hostId);                             // 404 for an unknown user, not an empty list
        List<ListingView> views = new ArrayList<>();
        for (Listing listing : listings.findByHostIdOrderByIdAsc(hostId)) {
            views.add(ListingView.of(listing));
        }
        return views;
    }

    /**
     * Search. Dates are optional, but come as a pair: with them, only listings free on those dates.
     *
     * @param sort "newest" (default), "price" (cheapest first) or "price_desc"
     */
    @Transactional(readOnly = true)
    public SearchPage search(String city, int guests, BigDecimal maxPrice, LocalDate checkIn, LocalDate checkOut,
                             String sort, int page, int size) {
        BigDecimal priceLimit = (maxPrice == null) ? NO_PRICE_LIMIT : maxPrice;
        PageRequest pageRequest = PageRequest.of(page, size, sortOrder(sort));

        Page<Listing> result;
        if (checkIn == null && checkOut == null) {
            result = listings.search(city.trim(), guests, priceLimit, pageRequest);
        } else {
            DateRange stay = new DateRange(checkIn, checkOut);   // 400 if only one date, or out before in
            if (stay.checkIn().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("checkIn must be today or later");
            }
            result = listings.searchAvailable(city.trim(), guests, priceLimit, stay.checkIn(), stay.checkOut(),
                    pageRequest);
        }

        List<ListingView> views = new ArrayList<>();
        for (Listing listing : result.getContent()) {
            views.add(ListingView.of(listing));
        }
        return new SearchPage(views, page, size, result.getTotalElements(), result.getTotalPages());
    }

    /**
     * Only these three sort orders. Passing the client's text straight into Sort.by(...) would let
     * anyone sort by any field, including ones that are slow to sort or not meant to be public.
     * The id at the end makes the order complete, so a listing never jumps between pages.
     */
    private static Sort sortOrder(String sort) {
        switch (sort) {
            case "newest":
                return Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
            case "price":
                return Sort.by(Sort.Order.asc("nightlyPrice"), Sort.Order.asc("id"));
            case "price_desc":
                return Sort.by(Sort.Order.desc("nightlyPrice"), Sort.Order.asc("id"));
            default:
                throw new IllegalArgumentException("sort must be newest, price or price_desc");
        }
    }

    private Listing find(long id) {
        return listings.findById(id).orElseThrow(() -> new NotFoundException("listing " + id + " not found"));
    }

    /** The listing, if hostId is its host; 403 for anyone else. */
    private Listing findOwnedBy(long id, long hostId) {
        Listing listing = find(id);
        if (listing.getHost().getId() != hostId) {
            throw new ForbiddenException("only the host of listing " + id + " can change it");
        }
        return listing;
    }

    private static Set<Amenity> amenities(ListingRequest request) {
        return request.amenities() == null ? new HashSet<>() : new HashSet<>(request.amenities());
    }
}
