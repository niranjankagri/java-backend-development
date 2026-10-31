package jbdl.session20.orders;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publishes order events to the Kafka topic {@value #TOPIC}. */
@Component
public class KafkaOrderEvents implements OrderEvents {

    public static final String TOPIC = "orders.placed";

    // KafkaTemplate is to Kafka what RestClient is to HTTP. Spring Boot creates it from the
    // spring.kafka.* settings; the key is a String, the value is turned into JSON.
    private final KafkaTemplate<String, OrderPlaced> kafka;

    public KafkaOrderEvents(KafkaTemplate<String, OrderPlaced> kafka) {
        this.kafka = kafka;
    }

    @Override
    public void publish(OrderPlaced event) {
        // The KEY decides the partition: all events of one order get the same key, so they land in
        // the same partition and are read in the order they were sent.
        String key = String.valueOf(event.orderId());

        // send() returns at once with a CompletableFuture (Session 5) that completes when the broker
        // has stored the event. join() waits for that, so a broker problem fails this request
        // instead of being lost silently. Simpler to read, but slower: a service that sends many
        // events would rather react to the future later than wait for each one.
        kafka.send(TOPIC, key, event).join();
    }
}
