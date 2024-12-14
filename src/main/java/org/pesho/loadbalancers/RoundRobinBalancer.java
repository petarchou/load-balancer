package org.pesho.loadbalancers;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinBalancer implements LoadBalancer {
    private volatile List<String> servers;
    private final AtomicInteger currentServer = new AtomicInteger();

    public RoundRobinBalancer() {
    }

    public String nextServer() {
        var serverRef = servers; // Keep the same reference for the whole call
        if (serverRef == null || serverRef.isEmpty()) {
            throw new IllegalStateException("No active servers available");
        }
        return serverRef.get(Math.abs(currentServer.getAndIncrement() % serverRef.size()));
    }

    @Override
    public void setServers(List<String> servers) {
        this.servers = servers;
    }
}
