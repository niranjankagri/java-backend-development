package jbdl.session09.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request an id and puts it in the logging MDC (Mapped Diagnostic Context), so every log
 * line written while handling the request carries it (see logging.pattern in application.properties).
 * Reuses the caller's X-Request-Id when it looks safe, otherwise creates one; returns it in the
 * response so a client can quote it in a bug report.
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    // never copy arbitrary client input into logs: only short ids made of safe characters
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String requestId;
        if (incoming != null && SAFE_ID.matcher(incoming).matches()) {
            requestId = incoming;                       // the client sent a usable id: keep it
        } else {
            requestId = UUID.randomUUID().toString();   // otherwise make a new random one
        }
        // MDC is a per-thread map that the logging library reads for every log line; %X{requestId}
        // in the log pattern prints this value. It works because one request runs on one thread.
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY); // threads are reused by Tomcat: never leak the id into the next request
        }
    }
}
