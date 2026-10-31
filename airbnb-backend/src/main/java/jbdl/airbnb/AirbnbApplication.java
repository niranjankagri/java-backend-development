package jbdl.airbnb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Airbnb-like rental backend: hosts offer listings, guests search them and (from Session 22)
 * book them. One Spring Boot application with one package per part of the business: users,
 * listings, bookings (Session 21, topic 2 explains why not microservices yet).
 *
 *   ./mvnw -pl airbnb-backend package
 *   java -jar airbnb-backend/target/airbnb-backend-1.0.0-SNAPSHOT.jar
 */
@SpringBootApplication
public class AirbnbApplication {

    public static void main(String[] args) {
        SpringApplication.run(AirbnbApplication.class, args);
    }
}
