package jbdl.session15.web;

import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A COOKIE, for comparison with a cache: a small value the server asks the browser to store and send
 * back with every request to this site. It belongs to ONE user (their preferred currency); a cache
 * holds shared data to save work.
 */
@RestController
@RequestMapping("/api/preferences")
public class PreferenceController {

    public static final String COOKIE = "currency";

    /** Remember the user's currency for 30 days. */
    @PutMapping("/currency/{currency}")
    public ResponseEntity<String> choose(@PathVariable String currency) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE, currency.toUpperCase())
                .httpOnly(true)              // JavaScript in the page can't read it (protects against XSS theft)
                .sameSite("Lax")             // not sent with most cross-site requests (protects against CSRF)
                .path("/")
                .maxAge(Duration.ofDays(30)) // without maxAge it would be deleted when the browser closes
                .build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body("saved");
    }

    /** The browser sends the cookie back automatically; default INR when there is none. */
    @GetMapping("/currency")
    public String current(@CookieValue(name = COOKIE, defaultValue = "INR") String currency) {
        return currency;
    }
}
