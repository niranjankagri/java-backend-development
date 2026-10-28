package jbdl.session09;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import jbdl.session09.aop.AuditTrail;
import jbdl.session09.aop.TransientFailureException;
import jbdl.session09.orders.Order;
import jbdl.session09.orders.OrderRequest;
import jbdl.session09.orders.OrderService;
import jbdl.session09.orders.PaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Topics 1-2 (AOP): the aspects run around the real beans. Each test calls ordinary business methods
 * and then checks what the aspects did: the audit trail, the retries, and the log lines
 * (OutputCaptureExtension collects everything printed to the console during the test).
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class AspectsTest {

    @Autowired
    private OrderService orders;

    @Autowired
    private PaymentGateway payments;

    @Autowired
    private AuditTrail trail;

    /** The beans are shared by all tests: reset their state first. */
    @BeforeEach
    void reset() {
        orders.clear();
        payments.reset();
        trail.clear();
    }

    private static OrderRequest request(String customer) {
        return new OrderRequest(customer, List.of(new OrderRequest.Line("pen", 2, new BigDecimal("25.00"))));
    }

    @Test
    @DisplayName("Spring hands out a proxy, not the plain OrderService object")
    void beanIsAProxy() {
        assertThat(AopUtils.isAopProxy(orders)).isTrue();
    }

    @Test
    @DisplayName("@Audited: a successful call writes one success entry")
    void auditOnSuccess() {
        // when
        Order order = orders.place(request("Asha"));

        // then
        assertThat(trail.entries()).hasSize(1);
        AuditTrail.Entry entry = trail.entries().get(0);
        assertThat(entry.action()).isEqualTo("ORDER_PLACED");
        assertThat(entry.success()).isTrue();
        assertThat(entry.detail()).contains("customer=Asha").contains("id=" + order.getId());
    }

    @Test
    @DisplayName("@Audited: a failed call writes a failure entry, and the exception still reaches the caller")
    void auditOnFailure() {
        // given: an order without lines is invalid
        OrderRequest empty = new OrderRequest("Ravi", List.of());

        // when / then
        assertThatThrownBy(() -> orders.place(empty)).isInstanceOf(IllegalArgumentException.class);
        assertThat(trail.entries()).singleElement().satisfies(entry -> {
            assertThat(entry.success()).isFalse();
            assertThat(entry.detail()).contains("an order needs at least one line");
        });
    }

    @Test
    @DisplayName("@Retryable: two timeouts, the third attempt succeeds")
    void retrySucceeds() {
        // given: the payment provider will fail twice
        payments.failNextCalls(2);

        // when
        Order order = orders.place(request("Asha"));

        // then: the order is paid and charge() really ran three times
        assertThat(order.getStatus()).isEqualTo(Order.Status.PAID);
        assertThat(payments.calls()).isEqualTo(3);
    }

    @Test
    @DisplayName("@Retryable: after 3 failed attempts the error reaches the caller")
    void retryGivesUp() {
        payments.failNextCalls(5);

        assertThatThrownBy(() -> orders.place(request("Asha"))).isInstanceOf(TransientFailureException.class);
        assertThat(payments.calls()).isEqualTo(3); // maxAttempts = 3, not 5
    }

    @Test
    @DisplayName("@Timed and the service trace write log lines")
    void timingAndTraceAreLogged(CapturedOutput output) {
        orders.place(request("Meena"));

        assertThat(output.getOut())
                .contains("OrderService.place(..) took")          // PerformanceAspect
                .contains("PaymentGateway.charge(..) took")
                .contains("-> OrderService.place(..)");           // ServiceTraceAspect (DEBUG)
    }

    @Test
    @DisplayName("Self-invocation trap: inner this.place(...) calls skip the aspects")
    void selfInvocationBypassesProxy() {
        // when: placeAll is called through the proxy, but it calls place() on `this` (the real object)
        int placed = orders.placeAll(List.of(request("A"), request("B")));

        // then: two orders exist, yet the audit aspect saw none of them
        assertThat(placed).isEqualTo(2);
        assertThat(trail.entries()).isEmpty();
    }
}
