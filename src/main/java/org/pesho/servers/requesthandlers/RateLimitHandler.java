package org.pesho.servers.requesthandlers;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.pesho.ratelimiting.ratelimiters.RateLimiter;

import java.io.IOException;

public class RateLimitHandler extends BaseHandler {
    private final RateLimiter rateLimiter;

    public RateLimitHandler(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String clientIp = getClientIp(request);

        if (rateLimiter.canProcess(clientIp)) {
            processNext(request, response);
        } else {
            response.setStatus(429);
            response.getWriter().write("Rate limit exceeded");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
