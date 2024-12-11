package org.pesho.servers.loadbalancing;

public interface LoadBalancer {
    String nextServer();
}
