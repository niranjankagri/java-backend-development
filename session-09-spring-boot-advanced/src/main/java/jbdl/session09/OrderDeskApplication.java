package jbdl.session09;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Order desk: a small order service whose cross-cutting concerns (timing, auditing, retries,
 * request ids in logs) live in aspects and a filter instead of inside the business code.
 * spring-boot-starter-aspectj on the classpath switches on @Aspect support automatically.
 */
@SpringBootApplication
public class OrderDeskApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderDeskApplication.class, args);
    }
}
