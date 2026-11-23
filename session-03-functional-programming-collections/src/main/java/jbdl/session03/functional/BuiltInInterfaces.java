package jbdl.session03.functional;

import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;

/**
 * The java.util.function interfaces FunctionalExamples doesn't cover: Supplier, the Bi- versions,
 * the Operator shortcuts and the primitive specialisations that avoid boxing.
 */
public final class BuiltInInterfaces {

    private BuiltInInterfaces() {
    }

    /** Supplier<T>: no input, returns a T. Good for lazy or repeated creation. */
    public static final Supplier<String> DEFAULT_CURRENCY = () -> "INR";

    /** BiFunction<T, U, R>: two inputs, one result. */
    public static final BiFunction<Integer, Integer, Integer> PERCENT_OF = (percent, amount) -> amount * percent / 100;

    /** UnaryOperator<T>: a Function whose input and output types are the same. */
    public static final UnaryOperator<String> TRIM_AND_LOWER = text -> text.trim().toLowerCase(Locale.ROOT);

    /** BinaryOperator<T>: a BiFunction with all three types the same; what reduce() takes. */
    // Same as: (a, b) -> a >= b ? a : b   (maxBy builds it from a comparator)
    public static final BinaryOperator<Integer> LARGER = BinaryOperator.maxBy(Integer::compare);

    /** IntPredicate: Predicate<Integer> without boxing every int into an Integer object. */
    public static final IntPredicate EVEN = n -> n % 2 == 0;

    /** ToIntFunction<T>: T in, a primitive int out (no Integer object). */
    public static final ToIntFunction<String> WORD_COUNT = text -> text.isBlank() ? 0 : text.trim().split("\\s+").length;

    /** Supplier is evaluated only when asked: the expensive default isn't built if a value exists. */
    public static String valueOrDefault(String value, Supplier<String> fallback) {
        return value != null ? value : fallback.get();
    }

    /** reduce with a BinaryOperator: the largest number, or the identity for an empty list. */
    public static int largest(List<Integer> numbers) {
        // reduce combines all elements two at a time, starting from MIN_VALUE. The same as a loop:
        //   int largest = Integer.MIN_VALUE;
        //   for (int n : numbers) { largest = LARGER.apply(largest, n); }
        return numbers.stream().reduce(Integer.MIN_VALUE, LARGER);
    }
}
