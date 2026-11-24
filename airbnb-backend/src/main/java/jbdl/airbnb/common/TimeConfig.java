package jbdl.airbnb.common;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Which day is "today"? LocalDate.now() answers in the SERVER's time zone: UTC on Render. In India it is
 * already the next day from 18:30 to midnight UTC, but the server still counted the old one: a check-in
 * on a day that had already ended in India was accepted, and cancelling and reviewing went by the wrong
 * day.
 *
 * The rules count days in the business's zone instead (prices are in INR), through one Clock bean that
 * the services ask: LocalDate.now(clock). A test can replace the bean with a fixed clock (Session 14
 * did the same for LoanService), see BusinessDayTest.
 */
@Configuration
public class TimeConfig {

    /** The zone the booking rules count days in. */
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    @Bean
    public Clock clock() {
        return Clock.system(BUSINESS_ZONE);
    }
}
