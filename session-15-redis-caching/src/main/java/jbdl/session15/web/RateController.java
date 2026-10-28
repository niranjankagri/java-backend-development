package jbdl.session15.web;

import java.math.BigDecimal;
import java.time.Duration;
import jbdl.session15.rates.RateService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

/**
 * Browser (HTTP) caching on top of the server cache:
 *   Cache-Control: max-age=60   the browser may reuse the answer for 60 s without asking again
 *   ETag: "USD-83.20"           a fingerprint of the answer; after 60 s the browser asks
 *                               "still USD-83.20?" (If-None-Match) and gets 304 Not Modified, no body
 */
@RestController
@RequestMapping("/api/rates")
public class RateController {

    private final RateService rates;

    public RateController(RateService rates) {
        this.rates = rates;
    }

    /** The answer the browser stores. */
    public record RateView(String currency, BigDecimal toInr) {
    }

    @GetMapping("/{currency}")
    public ResponseEntity<RateView> rate(@PathVariable String currency, WebRequest request) {
        String code = currency.toUpperCase();
        BigDecimal rate = rates.rateToInr(code);
        String etag = "\"" + code + "-" + rate.toPlainString() + "\"";

        // the browser already has exactly this version: answer 304 with an empty body
        if (request.checkNotModified(etag)) {
            return null; // Spring has already prepared the 304 response
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)))
                .eTag(etag)
                .body(new RateView(code, rate));
    }

    /** Changing a rate updates the server cache; browsers see the new ETag on their next check. */
    @PutMapping("/{currency}")
    public RateView update(@PathVariable String currency, @RequestBody BigDecimal newRate) {
        String code = currency.toUpperCase();
        return new RateView(code, rates.update(code, newRate));
    }
}
