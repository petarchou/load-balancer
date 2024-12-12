package org.pesho.health;

import com.google.gson.Gson;
import org.eclipse.jetty.http.HttpStatus;
import org.pesho.ServerInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.pesho.config.RedisClient.JEDIS;

public class HealthMonitor {
    private final ScheduledExecutorService scheduler;
    private final HttpClient client;
    private boolean initialized;

    public HealthMonitor() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        client =
                HttpClient.newBuilder().executor(Executors.newVirtualThreadPerTaskExecutor()).build();
    }

    public synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        Runnable pingServers = () -> {
            try {
                Gson gson = new Gson();
                Map<String, ServerInfo> servers = JEDIS.hgetAll("servers").entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                entry -> gson.fromJson(entry.getValue(), ServerInfo.class)));

                Set<CompletableFuture<HttpResponse<Void>>> responses = new HashSet<>();
                for (ServerInfo server : servers.values()) {
                    HttpRequest req = HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create("http://" + server + "/health"))
                            .timeout(Duration.ofSeconds(10))
                            .build();

                    var futureResponse = client.sendAsync(req,
                            BodyHandlers.discarding());
                    responses.add(futureResponse);
                }

                CompletableFuture.allOf(responses.toArray(new CompletableFuture[0]))
                        .thenRun(() -> {
                            for (CompletableFuture<HttpResponse<Void>> future : responses) {
                                future.thenAccept(response -> {
                                    String serverName =
                                            response.uri().getHost() + ":" + response.uri().getPort();
                                    if (response.statusCode() != HttpStatus.OK_200) {
                                        if (servers.get(serverName).getStatus().equals("active")) {
                                            var server = servers.get(serverName);
                                            server.setStatus("inactive");
                                            JEDIS.hset("servers", serverName, gson.toJson(server));
                                        }
                                    } else if (servers.get(serverName).getStatus().equals(
                                            "inactive")) {
                                        var server = servers.get(serverName);
                                        server.setStatus("active");
                                        JEDIS.hset("servers", serverName, gson.toJson(server));
                                    }
                                });
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }

        };

        scheduler.scheduleAtFixedRate(pingServers, 0, 10, TimeUnit.SECONDS);
    }
}
