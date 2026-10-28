package jbdl.session15;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Currency rates, served fast thanks to caching. @EnableCaching switches on @Cacheable and friends.
 * Which cache is used is configuration only:
 *   default             spring.cache.type=simple  -> an in-memory map inside this JVM
 *   --spring.profiles.active=redis               -> Redis (a separate server shared by all instances)
 */
@SpringBootApplication
@EnableCaching
public class RatesApplication {

    public static void main(String[] args) {
        SpringApplication.run(RatesApplication.class, args);
    }
}
