package jbdl.session25.compare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Topic 6: the tests every AI tool's solution must pass. The tools only saw PROBLEM.md, never these
 * tests, so the tests are an independent check of what each tool understood.
 *
 * <p>This class is abstract: it holds the tests but no solution. Each tool gets a small subclass
 * that says which calculator to test (ClaudePriceCalculatorTest, CodexPriceCalculatorTest), and
 * JUnit runs all the tests below once for each subclass. Adding a third tool = one more subclass.
 */
public abstract class PriceCalculatorContract {

    private static final BigDecimal PRICE = new BigDecimal("1000");

    // 2 Nov 2026 is a Monday, 6 Nov a Friday
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);
    private static final LocalDate FRIDAY = LocalDate.of(2026, 11, 6);

    private PriceCalculator calculator;

    /** The solution under test; each subclass returns its own. */
    protected abstract PriceCalculator createCalculator();

    @BeforeEach
    void setUp() {
        calculator = createCalculator();
    }

    @Test
    void oneWeekdayNightIsThePricePlusTheCleaningFee() {
        // Monday night: 1000 + 500
        assertThat(calculator.total(PRICE, MONDAY, MONDAY.plusDays(1))).isEqualTo(new BigDecimal("1500.00"));
    }

    @Test
    void fridayAndSaturdayNightsCostTwentyPercentMore() {
        // Friday and Saturday nights: 1200 + 1200 + 500
        assertThat(calculator.total(PRICE, FRIDAY, FRIDAY.plusDays(2))).isEqualTo(new BigDecimal("2900.00"));
    }

    @Test
    void sixNightsGetNoDiscount() {
        // Monday to Saturday nights: 4 x 1000 + 2 x 1200 + 500
        assertThat(calculator.total(PRICE, MONDAY, MONDAY.plusDays(6))).isEqualTo(new BigDecimal("6900.00"));
    }

    @Test
    void sevenNightsGetTenPercentOffTheNightsButNotOffTheCleaningFee() {
        // (5 x 1000 + 2 x 1200) x 0.9 + 500 = 6660 + 500
        assertThat(calculator.total(PRICE, MONDAY, MONDAY.plusDays(7))).isEqualTo(new BigDecimal("7160.00"));
    }

    @Test
    void thirtyNightsAreAllowed() {
        // 1 to 30 Nov 2026: 8 Friday or Saturday nights and 22 other nights
        // (22 x 1000 + 8 x 1200) x 0.9 + 500 = 28440 + 500
        assertThat(calculator.total(PRICE, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1)))
                .isEqualTo(new BigDecimal("28940.00"));
    }

    @Test
    void aStayCanCrossTheEndOfTheYear() {
        // Thursday 31 Dec 2026 and Friday 1 Jan 2027: 1000 + 1200 + 500
        assertThat(calculator.total(PRICE, LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 2)))
                .isEqualTo(new BigDecimal("2700.00"));
    }

    @Test
    void onlyTheFinalTotalIsRounded() {
        // 7 nights at 10.01: 5 x 10.01 + 2 x 12.012 = 74.074; x 0.9 = 66.6666; + 500 = 566.6666 -> 566.67
        // Rounding every night first (12.012 -> 12.01) would give 566.66
        assertThat(calculator.total(new BigDecimal("10.01"), MONDAY, MONDAY.plusDays(7)))
                .isEqualTo(new BigDecimal("566.67"));
    }

    @Test
    void theResultAlwaysHasTwoDecimals() {
        // isEqualTo on BigDecimal also compares the scale: 1500 and 1500.00 are not equal
        assertThat(calculator.total(new BigDecimal("1000"), MONDAY, MONDAY.plusDays(1)).scale()).isEqualTo(2);
    }

    @Test
    void checkOutMustBeAfterCheckIn() {
        assertThatThrownBy(() -> calculator.total(PRICE, MONDAY, MONDAY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.total(PRICE, MONDAY, MONDAY.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void thirtyOneNightsAreTooMany() {
        assertThatThrownBy(() -> calculator.total(PRICE, MONDAY, MONDAY.plusDays(31)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theNightlyPriceMustBeGreaterThanZero() {
        assertThatThrownBy(() -> calculator.total(BigDecimal.ZERO, MONDAY, MONDAY.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.total(new BigDecimal("-1"), MONDAY, MONDAY.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullArgumentsAreRejectedWithIllegalArgumentException() {
        // the problem asks for IllegalArgumentException, not the NullPointerException Java would throw
        assertThatThrownBy(() -> calculator.total(null, MONDAY, MONDAY.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.total(PRICE, null, MONDAY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.total(PRICE, MONDAY, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
