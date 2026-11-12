package jbdl.session20.shipping;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** GET /rates/{city}: the shipping fee to that city, in INR. */
@RestController
public class ShippingController {

    /** Cities with their own fee; every other city pays {@link #OTHER_CITIES}. */
    private static final Map<String, BigDecimal> FEES = Map.of(
            "delhi", new BigDecimal("40"),
            "mumbai", new BigDecimal("60"),
            "haldwani", new BigDecimal("80"));
    private static final BigDecimal OTHER_CITIES = new BigDecimal("120");

    @GetMapping("/rates/{city}")
    public ShippingRate rate(@PathVariable String city) {
        BigDecimal fee = FEES.getOrDefault(city.toLowerCase(), OTHER_CITIES);
        return new ShippingRate(city, fee);
    }

    /** The answer: {"city":"Delhi","fee":40} */
    public record ShippingRate(String city, BigDecimal fee) {
    }
}
