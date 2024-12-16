package org.pesho.ratelimiting.ratelimiters;

public interface RateLimiter {
    public boolean canProcess(String ip);
}
