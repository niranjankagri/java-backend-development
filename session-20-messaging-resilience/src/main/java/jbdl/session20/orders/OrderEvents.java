package jbdl.session20.orders;

/**
 * Where order events go. {@link KafkaOrderEvents} sends them to Kafka; a test can use a lambda that
 * keeps them in a list instead (one method, so a lambda is enough, as in Session 19).
 */
public interface OrderEvents {

    void publish(OrderPlaced event);
}
