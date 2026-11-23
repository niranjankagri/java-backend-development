package jbdl.session04.streams;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Typical backend reports written with the Streams API. A stream is a pipeline: a source (a list),
 * intermediate operations (filter, map, sorted: lazy, return a new stream) and one terminal
 * operation (collect, sum, findFirst: runs the pipeline). A stream can be used only once.
 */
public final class OrderReports {

    private OrderReports() {
    }

    /** filter + map + sorted + collect: customers with delivered orders, A-Z, no duplicates. */
    public static List<String> deliveredCustomers(List<Order> orders) {
        return orders.stream()
                .filter(order -> order.status() == Order.Status.DELIVERED)
                .map(Order::customer)
                .distinct()
                .sorted()
                .toList();
    }

    /** mapToLong + sum: revenue of every order that wasn't cancelled. */
    public static long revenue(List<Order> orders) {
        return orders.stream()
                .filter(order -> order.status() != Order.Status.CANCELLED)
                .mapToLong(Order::total)
                .sum();
    }

    /** groupingBy with a downstream collector: revenue per city, cities sorted (TreeMap). */
    public static Map<String, Long> revenueByCity(List<Order> orders) {
        // Same with a loop:
        //   Map<String, Long> result = new TreeMap<>();
        //   for (Order order : orders) {
        //       if (order.status() != Order.Status.CANCELLED) {
        //           result.merge(order.city(), order.total(), Long::sum);   // add to the city's total
        //       }
        //   }
        return orders.stream()
                .filter(order -> order.status() != Order.Status.CANCELLED)
                .collect(Collectors.groupingBy(Order::city, TreeMap::new, Collectors.summingLong(Order::total)));
    }

    /** groupingBy + counting: how many orders in each status. */
    public static Map<Order.Status, Long> countByStatus(List<Order> orders) {
        // Same with a loop: for each order, counts.merge(order.status(), 1L, Long::sum)
        return orders.stream().collect(Collectors.groupingBy(Order::status, Collectors.counting()));
    }

    /** partitioningBy: always two groups, true and false. */
    public static Map<Boolean, List<Integer>> bigOrderIds(List<Order> orders, long threshold) {
        // Same with a loop: two lists, add each order's id to the "big" or the "small" one,
        // then put them in a map under the keys true and false.
        return orders.stream().collect(Collectors.partitioningBy(order -> order.total() >= threshold,
                Collectors.mapping(Order::id, Collectors.toList())));
    }

    /** flatMap: one stream of all line items across all orders (not cancelled ones), then the best-selling product. */
    public static Optional<String> bestSellingProduct(List<Order> orders) {
        // The most advanced pipeline here. Step by step, as loops:
        //   1. Map<String, Integer> sold = new HashMap<>();
        //      for (Order order : orders) if (order.status() != CANCELLED) for (Item item : order.items()) sold.merge(...);
        //   2. walk sold.entrySet() and keep the entry with the highest quantity (ties: the larger name);
        //   3. return its key, or Optional.empty() if there were no items.
        // A cancelled order sold nothing, so it is skipped, the same as in revenue().
        return orders.stream()
                .filter(order -> order.status() != Order.Status.CANCELLED)
                .flatMap(order -> order.items().stream())
                .collect(Collectors.groupingBy(Order.Item::product, Collectors.summingInt(Order.Item::quantity)))
                .entrySet().stream()
                .max(Map.Entry.<String, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey);
    }

    /** sorted with a comparator + limit: the top N orders by value. */
    public static List<Integer> topOrderIds(List<Order> orders, int n) {
        // Same with a loop: copy the list, sort it with the same comparator, then take the ids of the
        // first n orders. comparingLong(Order::total).reversed() = by total, highest first.
        return orders.stream()
                .sorted(Comparator.comparingLong(Order::total).reversed())
                .limit(n)
                .map(Order::id)
                .toList();
    }

    /** joining: a comma-separated line, e.g. for a CSV or a log message. */
    public static String citiesLine(List<Order> orders) {
        return orders.stream().map(Order::city).distinct().sorted().collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * Laziness: nothing runs until the terminal operation, and findFirst stops early. The visitor
     * records which orders the filter actually looked at.
     */
    public static Optional<Integer> firstCancelled(List<Order> orders, Consumer<Integer> visitor) {
        return orders.stream()
                .peek(order -> visitor.accept(order.id()))
                .filter(order -> order.status() == Order.Status.CANCELLED)
                .map(Order::id)
                .findFirst();
    }
}
