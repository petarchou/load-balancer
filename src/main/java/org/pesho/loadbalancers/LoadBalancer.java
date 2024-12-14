package org.pesho.loadbalancers;

import java.util.List;

public interface LoadBalancer {
    String nextServer();
    void setServers(List<String> servers);
}
