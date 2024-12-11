package org.pesho.servers.loadbalancing;

import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinBalancer implements LoadBalancer {
    private final String[] servers;
    private final AtomicInteger currentServer = new AtomicInteger();

    public RoundRobinBalancer(String[] servers) {
        this.servers = servers;
    }

    public String nextServer() {
        return servers[Math.abs(currentServer.getAndIncrement() % servers.length)];
    }
}
