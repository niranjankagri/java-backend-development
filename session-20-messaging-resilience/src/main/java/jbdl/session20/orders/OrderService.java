package jbdl.session20.orders;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Places orders: one SYNCHRONOUS call (shipping fee: the order needs the answer now) and one
 * ASYNCHRONOUS message (OrderPlaced: order-service does not wait for anyone to read it). Topic 1
 * explains when to use which.
 */
@Service
public class OrderService {

    private final ShippingFees shippingFees;
    private final OrderEvents events;
    private final BigDecimal freeShippingFrom;
    // Orders are kept in memory to keep the session about messaging; Session 11 showed how to store them.
    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    // freeShippingFrom comes from config-server when it runs (topic 3), else from application-orders.properties
    public OrderService(ShippingFees shippingFees, OrderEvents events,
                        @Value("${orders.free-shipping-from}") BigDecimal freeShippingFrom) {
        this.shippingFees = shippingFees;
        this.events = events;
        this.freeShippingFrom = freeShippingFrom;
    }

    public Order place(OrderRequest request) {
        if (request.customerEmail() == null || request.customerEmail().isBlank()) {
            throw new IllegalArgumentException("customerEmail is required");
        }
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be more than 0");
        }

        ShippingQuote shipping;
        if (request.amount().compareTo(freeShippingFrom) >= 0) {
            shipping = new ShippingQuote(BigDecimal.ZERO, "free");    // no need to ask shipping-service
        } else {
            shipping = shippingFees.quote(request.city());
        }

        BigDecimal total = request.amount().add(shipping.fee());
        Order order = new Order(nextId.getAndIncrement(), request.customerEmail(), request.city(),
                request.amount(), shipping.fee(), shipping.source(), total);
        orders.put(order.id(), order);

        // Tell the world. A new random event id per event, so consumers can spot duplicates (topic 2).
        events.publish(new OrderPlaced(UUID.randomUUID().toString(), order.id(), order.customerEmail(), total));
        return order;
    }

    public Order find(long id) {
        Order order = orders.get(id);
        if (order == null) {
            throw new OrderNotFoundException(id);
        }
        return order;
    }
}
