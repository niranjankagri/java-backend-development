package jbdl.session20.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * OrderService without Spring, Kafka or shipping-service. Lambdas are enough: a fake shipping-service
 * with a fixed fee, and a fake event publisher that keeps the events in a list, behind a real
 * OrderOutbox. place() only puts the event in the outbox; relay() is what sends it (OrderOutboxTest).
 */
class OrderServiceTest {

    private final List<OrderPlaced> published = new ArrayList<>();
    private final ShippingFees fixedFee = city -> new ShippingQuote(new BigDecimal("40"), "shipping-service");
    private final OrderOutbox outbox = new OrderOutbox(published::add);
    // free shipping from 500
    private final OrderService service = new OrderService(fixedFee, outbox, new BigDecimal("500"));

    @Test
    @DisplayName("Below the free-shipping amount: fee from shipping-service, total = amount + fee")
    void withShippingFee() {
        Order order = service.place(new OrderRequest("asha@example.com", "Delhi", new BigDecimal("450")));

        assertThat(order.shippingFee()).isEqualByComparingTo("40");
        assertThat(order.shippingFeeSource()).isEqualTo("shipping-service");
        assertThat(order.total()).isEqualByComparingTo("490");
    }

    @Test
    @DisplayName("From the free-shipping amount: fee 0, and shipping-service is not asked")
    void freeShipping() {
        ShippingFees mustNotBeCalled = city -> {
            throw new AssertionError("shipping-service was asked");
        };
        OrderService strictService = new OrderService(mustNotBeCalled, outbox, new BigDecimal("500"));

        Order order = strictService.place(new OrderRequest("asha@example.com", "Delhi", new BigDecimal("500")));

        assertThat(order.shippingFee()).isEqualByComparingTo("0");
        assertThat(order.shippingFeeSource()).isEqualTo("free");
        assertThat(order.total()).isEqualByComparingTo("500");
    }

    @Test
    @DisplayName("Every placed order publishes one OrderPlaced event with its id, e-mail and total")
    void publishesEvent() {
        Order order = service.place(new OrderRequest("asha@example.com", "Delhi", new BigDecimal("450")));
        assertThat(outbox.pending()).hasSize(1);
        assertThat(published).isEmpty();          // not sent yet: that is the relay's job

        assertThat(outbox.relay()).isEqualTo(1);

        assertThat(outbox.pending()).isEmpty();
        assertThat(published).hasSize(1);
        OrderPlaced event = published.get(0);
        assertThat(event.orderId()).isEqualTo(order.id());
        assertThat(event.customerEmail()).isEqualTo("asha@example.com");
        assertThat(event.total()).isEqualByComparingTo("490");
        assertThat(event.eventId()).isNotBlank();
    }

    @Test
    @DisplayName("Two orders: two events with different event ids")
    void newEventIdPerEvent() {
        service.place(new OrderRequest("asha@example.com", "Delhi", new BigDecimal("450")));
        service.place(new OrderRequest("asha@example.com", "Delhi", new BigDecimal("450")));
        outbox.relay();

        assertThat(published.get(0).eventId()).isNotEqualTo(published.get(1).eventId());
    }

    @Test
    @DisplayName("Missing e-mail or amount 0: refused, and no event is published")
    void invalidOrders() {
        assertThatThrownBy(() -> service.place(new OrderRequest(" ", "Delhi", new BigDecimal("450"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("customerEmail is required");
        assertThatThrownBy(() -> service.place(new OrderRequest("asha@example.com", "Delhi", BigDecimal.ZERO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("amount must be more than 0");

        assertThat(outbox.pending()).isEmpty();
    }
}
