package org.pesho.loadbalancers;

public interface LoadBalancer {
    String nextServer();
}
