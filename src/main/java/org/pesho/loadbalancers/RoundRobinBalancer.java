package org.pesho.loadbalancers;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinBalancer implements LoadBalancer {
    private final List<String> servers;
    private final AtomicInteger currentServer = new AtomicInteger();

    public RoundRobinBalancer(String... servers) {
        this.servers = new CopyOnWriteArrayList<>(List.of(servers));
    }

    public String nextServer() {
        return servers.get(Math.abs(currentServer.getAndIncrement() % servers.size()));
    }
}
