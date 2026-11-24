package jbdl.session20.orders;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The outbox relay without Spring or Kafka. The fake Kafka keeps what it receives in a list and can be
 * switched off: then publish() throws, as KafkaOrderEvents does when the broker does not answer.
 */
class OrderOutboxTest {

    /** A Kafka stand-in: down = every publish fails; failNext = only the next one fails. */
    static final class FakeKafka implements OrderEvents {

        final List<OrderPlaced> received = new ArrayList<>();
        boolean down;
        boolean failNext;

        @Override
        public void publish(OrderPlaced event) {
            if (down || failNext) {
                failNext = false;
                throw new IllegalStateException("broker not available");
            }
            received.add(event);
        }

        List<Long> orderIds() {
            return received.stream().map(OrderPlaced::orderId).toList();
        }
    }

    private final FakeKafka kafka = new FakeKafka();
    private final OrderOutbox outbox = new OrderOutbox(kafka);
    private final OrderService service = new OrderService(
            city -> new ShippingQuote(new BigDecimal("40"), "shipping-service"), outbox, new BigDecimal("500"));

    @Test
    @DisplayName("Kafka down: the order is still placed and its event waits; once Kafka is back the relay sends it")
    void kafkaDownKeepsTheEvent() {
        kafka.down = true;

        Order order = service.place(new OrderRequest("asha@example.com", "Delhi", new BigDecimal("450")));

        assertThat(service.find(order.id())).isEqualTo(order);
        assertThat(outbox.relay()).isZero();
        assertThat(outbox.relay()).isZero();               // tried again, still down, still kept
        assertThat(outbox.pending()).singleElement().extracting(OrderPlaced::orderId).isEqualTo(order.id());

        kafka.down = false;

        assertThat(outbox.relay()).isEqualTo(1);
        assertThat(kafka.orderIds()).containsExactly(order.id());
        assertThat(outbox.pending()).isEmpty();
    }

    @Test
    @DisplayName("Events wait in order and are sent oldest first")
    void oldestFirst() {
        kafka.down = true;
        long first = service.place(new OrderRequest("asha@example.com", "Delhi", new BigDecimal("450"))).id();
        long second = service.place(new OrderRequest("ravi@example.com", "Pune", new BigDecimal("700"))).id();
        long third = service.place(new OrderRequest("meera@example.com", "Goa", new BigDecimal("300"))).id();

        kafka.down = false;

        assertThat(outbox.relay()).isEqualTo(3);
        assertThat(kafka.orderIds()).containsExactly(first, second, third);
    }

    @Test
    @DisplayName("A failure stops the run: the failed event is not skipped, it goes first next time")
    void failureIsNotSkipped() {
        kafka.down = true;
        long first = service.place(new OrderRequest("asha@example.com", "Delhi", new BigDecimal("450"))).id();
        long second = service.place(new OrderRequest("ravi@example.com", "Pune", new BigDecimal("700"))).id();
        kafka.down = false;
        kafka.failNext = true;

        assertThat(outbox.relay()).isZero();               // the first one failed, so the second was not tried
        assertThat(kafka.received).isEmpty();

        assertThat(outbox.relay()).isEqualTo(2);
        assertThat(kafka.orderIds()).containsExactly(first, second);
    }

    @Test
    @DisplayName("Nothing waiting: the relay sends nothing")
    void emptyOutbox() {
        assertThat(outbox.relay()).isZero();
        assertThat(kafka.received).isEmpty();
    }
}
