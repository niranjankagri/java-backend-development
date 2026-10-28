package jbdl.session14.loans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sends messages to members. Here it only logs; a real one would send e-mail or SMS, which is exactly
 * the kind of dependency you never want a unit test to call for real (Mockito replaces it).
 */
@Component
public class Notifier {

    private static final Logger log = LoggerFactory.getLogger(Notifier.class);

    public void send(String member, String message) {
        log.info("to {}: {}", member, message);
    }
}
