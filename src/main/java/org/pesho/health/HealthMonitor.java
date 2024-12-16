package org.pesho.health;

import com.google.gson.Gson;
import org.eclipse.jetty.http.HttpStatus;
import org.pesho.models.ServerInfo;
import org.pesho.config.Environment;
import org.pesho.loadbalancers.LoadBalancer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.pesho.config.RedisClient.JEDIS;

public class HealthMonitor {
    private final ScheduledExecutorService scheduler;
    private final HttpClient client;
    private boolean initialized;

    private final LoadBalancer activeLoadBalancer;

    public HealthMonitor(LoadBalancer loadBalancer) {
        this.activeLoadBalancer = loadBalancer;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        client =
                HttpClient.newBuilder().executor(Executors.newVirtualThreadPerTaskExecutor()).build();
        System.out.println("Health monitor started");
    }

    public synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        scheduler.scheduleAtFixedRate(this::pingServers, 0,
                Environment.getIntProperty("HEALTH_INTERVAL_MILLIS"), TimeUnit.MILLISECONDS);
    }

    private void pingServers() {
        try {
            Gson gson = new Gson();
            Map<String, ServerInfo> servers = JEDIS.hgetAll("servers").entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            entry -> gson.fromJson(entry.getValue(), ServerInfo.class)));

            for (ServerInfo server : servers.values()) {
                HttpRequest req = HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create("http://" + server + "/health"))
                        .timeout(Duration.ofSeconds(10))
                        .build();

                client.sendAsync(req,
                                BodyHandlers.discarding())
                        .handle((response, throwable) -> {
                            if (throwable != null || response.statusCode() != HttpStatus.OK_200) {
                                // Server is down or responded with error
                                if (server.getStatus().equals("active")) {
                                    server.setStatus("inactive");
                                    JEDIS.hset("servers", server.toString(), gson.toJson(server));
                                }
                            } else {
                                // Server responded OK
                                if (server.getStatus().equals("inactive")) {
                                    server.setStatus("active");
                                    JEDIS.hset("servers", server.toString(), gson.toJson(server));
                                }
                            }
                            return response;
                        });
            }

            List<String> serverUris = servers.keySet().stream()
                    .filter(key -> servers.get(key).getStatus().equals("active"))
                    .toList();
            activeLoadBalancer.setServers(serverUris);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
