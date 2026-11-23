package jbdl.session04.streams;

import static jbdl.session04.testdata.TestOrders.ORDERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Topic 2: every report of OrderReports on the five shared test orders (TestOrders), plus laziness
 * (findFirst stops early) and single use of a stream.
 */
class OrderReportsTest {

    @Test
    @DisplayName("filter + map + distinct + sorted")
    void deliveredCustomers() {
        assertThat(OrderReports.deliveredCustomers(ORDERS)).containsExactly("Asha", "Kiran", "Meena");
    }

    @Test
    @DisplayName("mapToLong + sum skips cancelled orders")
    void revenue() {
        assertThat(OrderReports.revenue(ORDERS)).isEqualTo(2_400 + 1_500 + 6_000 + 600);
    }

    @Test
    @DisplayName("groupingBy with summing, and with counting")
    void grouping() {
        assertThat(OrderReports.revenueByCity(ORDERS))
                .containsExactly(entry("Bengaluru", 6_000L), entry("Delhi", 2_100L), entry("Pune", 2_400L));
        assertThat(OrderReports.countByStatus(ORDERS))
                .containsEntry(Order.Status.DELIVERED, 3L)
                .containsEntry(Order.Status.SHIPPED, 1L)
                .containsEntry(Order.Status.CANCELLED, 1L)
                .doesNotContainKey(Order.Status.PLACED);
    }

    @Test
    @DisplayName("partitioningBy always gives a true and a false group")
    void partitioning() {
        assertThat(OrderReports.bigOrderIds(ORDERS, 2_000))
                .containsEntry(true, List.of(1, 4))
                .containsEntry(false, List.of(2, 3, 5));
    }

    @Test
    @DisplayName("flatMap over line items: pens sold most (10 + 30; the cancelled 45 do not count)")
    void flatMap() {
        assertThat(OrderReports.bestSellingProduct(ORDERS)).contains("pen");
        assertThat(OrderReports.bestSellingProduct(List.of())).isEmpty();
    }

    @Test
    @DisplayName("A cancelled order's items are not sold: they cannot make a product the best seller")
    void cancelledItemsDoNotCount() {
        List<Order> orders = List.of(
                new Order(1, "Asha", "Pune", Order.Status.DELIVERED, List.of(new Order.Item("pen", 5, 20))),
                new Order(2, "Ravi", "Delhi", Order.Status.CANCELLED, List.of(new Order.Item("stapler", 50, 90))));
        assertThat(OrderReports.bestSellingProduct(orders)).contains("pen");
    }

    @Test
    @DisplayName("sorted by a comparator + limit, and joining")
    void topAndJoining() {
        assertThat(OrderReports.topOrderIds(ORDERS, 2)).containsExactly(4, 1);
        assertThat(OrderReports.citiesLine(ORDERS)).isEqualTo("[Bengaluru, Delhi, Pune]");
    }

    @Test
    @DisplayName("Lazy: findFirst stops at the first match; later orders are never looked at")
    void laziness() {
        // the peek in firstCancelled records every order the pipeline actually looked at
        List<Integer> visited = new ArrayList<>();

        assertThat(OrderReports.firstCancelled(ORDERS, visited::add)).contains(3);
        assertThat(visited).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("A stream can be consumed only once")
    void singleUse() {
        Stream<Order> stream = ORDERS.stream();
        stream.count();

        org.assertj.core.api.Assertions.assertThatThrownBy(stream::count).isInstanceOf(IllegalStateException.class);
    }
}
