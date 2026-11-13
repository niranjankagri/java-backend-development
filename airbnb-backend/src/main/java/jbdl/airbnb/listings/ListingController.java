package jbdl.airbnb.listings;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import jbdl.airbnb.listings.ListingDtos.ListingRequest;
import jbdl.airbnb.listings.ListingDtos.ListingView;
import jbdl.airbnb.listings.ListingDtos.SearchPage;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The listing API. The constraints on the search parameters (@Min, @Max ...) are checked by Spring
 * MVC before the method runs; a wrong value answers 400.
 */
@RestController
public class ListingController {

    private final ListingService service;

    public ListingController(ListingService service) {
        this.service = service;
    }

    @PostMapping("/api/listings")
    @ResponseStatus(HttpStatus.CREATED)
    public ListingView create(@Valid @RequestBody ListingRequest request) {
        return service.create(request);
    }

    @GetMapping("/api/listings/{id}")
    public ListingView get(@PathVariable long id) {
        return service.get(id);
    }

    @PutMapping("/api/listings/{id}")
    public ListingView update(@PathVariable long id, @Valid @RequestBody ListingRequest request) {
        return service.update(id, request);
    }

    /** Deactivates (never deletes): DELETE /api/listings/7?hostId=1 */
    @DeleteMapping("/api/listings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable long id, @RequestParam long hostId) {
        service.deactivate(id, hostId);
    }

    @GetMapping("/api/users/{hostId}/listings")
    public List<ListingView> listingsOf(@PathVariable long hostId) {
        return service.listingsOf(hostId);
    }

    /**
     * GET /api/listings/search?city=Goa&guests=2&checkIn=2026-12-20&checkOut=2026-12-23&maxPrice=5000&sort=price&page=0&size=10
     * Only city is required.
     */
    @GetMapping("/api/listings/search")
    public SearchPage search(
            @RequestParam @NotBlank String city,
            @RequestParam(defaultValue = "1") @Min(1) @Max(16) int guests,
            @RequestParam(required = false) @Positive BigDecimal maxPrice,
            // ISO dates: 2026-12-20
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return service.search(city, guests, maxPrice, checkIn, checkOut, sort, page, size);
    }
}
