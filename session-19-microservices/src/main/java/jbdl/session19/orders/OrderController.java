package jbdl.session19.orders;

import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** order-service's API. */
@RestController
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    /** 201 Created with the order and its address in the Location header. */
    @PostMapping("/orders")
    public ResponseEntity<Order> place(@RequestBody OrderRequest request) {
        Order order = orders.place(request);
        return ResponseEntity.created(URI.create("/orders/" + order.id())).body(order);
    }

    @GetMapping("/orders/{id}")
    public Order one(@PathVariable long id) {
        return orders.find(id);
    }
}
