package jbdl.session20.orders;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** POST /orders, GET /orders/{id}, and GET /shipping/circuit-breaker to watch the breaker (topic 4). */
@RestController
public class OrderController {

    private final OrderService orders;
    private final CircuitBreaker shippingCircuitBreaker;

    public OrderController(OrderService orders, CircuitBreaker shippingCircuitBreaker) {
        this.orders = orders;
        this.shippingCircuitBreaker = shippingCircuitBreaker;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public Order place(@RequestBody OrderRequest request) {
        return orders.place(request);
    }

    @GetMapping("/orders/{id}")
    public Order find(@PathVariable long id) {
        return orders.find(id);
    }

    /** The breaker's state and what it counted: {"state":"OPEN","failedCalls":4,...} */
    @GetMapping("/shipping/circuit-breaker")
    public Map<String, Object> circuitBreaker() {
        CircuitBreaker.Metrics metrics = shippingCircuitBreaker.getMetrics();
        Map<String, Object> answer = new LinkedHashMap<>();   // LinkedHashMap keeps this order in the JSON
        answer.put("state", shippingCircuitBreaker.getState().name());
        answer.put("failureRate", metrics.getFailureRate());  // -1 until 4 calls were made
        answer.put("successfulCalls", metrics.getNumberOfSuccessfulCalls());
        answer.put("failedCalls", metrics.getNumberOfFailedCalls());
        answer.put("refusedCalls", metrics.getNumberOfNotPermittedCalls());
        return answer;
    }
}
