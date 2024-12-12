package org.pesho.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.UnifiedJedis;

public class RedisClient {
    public static UnifiedJedis JEDIS;

    public static synchronized void connect() {
        if (JEDIS != null) return;

        JedisClientConfig config = DefaultJedisClientConfig.builder()
                .user(Environment.getJedisUser())
                .password(Environment.getJedisPassword())
                .build();

        JEDIS = new UnifiedJedis(
                new HostAndPort(Environment.getJedisUrl(), Environment.getJedisPort()),
                config
        );

        setUpServers();
        System.out.println("Connected to Redis");
    }

    private static synchronized void setUpServers() {
        Gson gson = new Gson();
        JsonObject serverInfo = new JsonObject();
        serverInfo.add("host", new JsonPrimitive("localhost"));
        serverInfo.add("port", new JsonPrimitive("8080"));
        serverInfo.add("status", new JsonPrimitive("active"));
        JEDIS.hset("servers", "localhost:8080", gson.toJson(serverInfo));
        serverInfo.add("port", new JsonPrimitive("8081"));
        JEDIS.hset("servers", "localhost:8081", gson.toJson(serverInfo));
    }
}

