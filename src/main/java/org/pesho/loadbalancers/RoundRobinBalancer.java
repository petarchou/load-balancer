package org.pesho.loadbalancers;

import com.google.gson.Gson;
import org.pesho.ServerInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.pesho.config.RedisClient.JEDIS;

public class RoundRobinBalancer implements LoadBalancer {
    private volatile List<String> servers;
    private final AtomicInteger currentServer = new AtomicInteger();

    public RoundRobinBalancer(long refreshIntervalMillis) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "server-health-checker");
            t.setDaemon(true);
            return t;
        });
        // Schedule periodic refresh
        scheduler.scheduleAtFixedRate(
                this::refreshServerList,
                0,
                refreshIntervalMillis,
                TimeUnit.MILLISECONDS
        );
    }

    public String nextServer() {
        var serverRef = servers; // Keep the same reference for the whole call
        if (serverRef == null || serverRef.isEmpty()) {
            throw new IllegalStateException("No active servers available");
        }
        return serverRef.get(Math.abs(currentServer.getAndIncrement() % serverRef.size()));
    }

    private void refreshServerList() {
        Map<String, String> servers = JEDIS.hgetAll("servers");
        Gson gson = new Gson();

        this.servers = servers.values().stream()
                .map(json -> gson.fromJson(json, ServerInfo.class))
                .filter(server -> "active".equals(server.getStatus()))
                .map(ServerInfo::toString)
                .collect(Collectors.toList());
    }
}
