package jbdl.session19.orders;

import feign.FeignException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/**
 * Places orders. The interesting part is the remote call: unlike a method call inside one program,
 * a call to another service can fail because the OTHER service is down or slow. Every remote call
 * needs an answer to "what if it fails?" (here: 503; Session 20 adds retries and circuit breakers).
 */
@Service
public class OrderService {

    private final CatalogClient catalog;
    // Orders are kept in memory to keep the session about services; Session 11 showed how to store them.
    // ConcurrentHashMap and AtomicLong: requests arrive on many threads at once (Session 5).
    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public OrderService(CatalogClient catalog) {
        this.catalog = catalog;
    }

    public Order place(OrderRequest request) {
        if (request.quantity() < 1) {
            throw new IllegalArgumentException("quantity must be at least 1");
        }
        CatalogProduct product = fetchProduct(request.productId());

        BigDecimal total = product.price().multiply(BigDecimal.valueOf(request.quantity()));
        Order order = new Order(nextId.getAndIncrement(), product.id(), product.name(), request.quantity(),
                product.price(), total, product.servedBy());
        orders.put(order.id(), order);
        return order;
    }

    public Order find(long id) {
        Order order = orders.get(id);
        if (order == null) {
            throw new OrderNotFoundException(id);
        }
        return order;
    }

    /** One remote call, with its failures turned into OUR exceptions (callers need not know Feign). */
    private CatalogProduct fetchProduct(long productId) {
        try {
            return catalog.product(productId);
        } catch (FeignException.NotFound e) {
            // the catalog answered "no such product": a normal answer, not a failure of the catalog
            throw new ProductNotFoundException(productId);
        } catch (FeignException e) {
            // everything else: no instance reachable (connection refused), timeout, 5xx answer ...
            throw new CatalogUnavailableException(e);
        }
    }
}
