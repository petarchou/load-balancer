package org.pesho.servers.servlets;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.pesho.servers.requesthandlers.LoadBalancerHandler;
import org.pesho.servers.requesthandlers.RateLimitHandler;
import org.pesho.servers.requesthandlers.RequestHandler;
import org.pesho.loadbalancers.LoadBalancer;
import org.pesho.ratelimiting.ratelimiters.RateLimiter;
import org.pesho.ratelimiting.ratelimiters.distributed.RedisSortedSetRateLimiter;

import java.io.IOException;

public class ResenderServlet extends HttpServlet {

    private RequestHandler requestChain;

    @Override
    public void init(ServletConfig config) {
        RateLimiter rateLimiter = new RedisSortedSetRateLimiter(20, 3);
        RequestHandler rateLimitHandler = new RateLimitHandler(rateLimiter);

        LoadBalancer loadBalancer = (LoadBalancer) config.getServletContext().getAttribute("loadBalancer");
        RequestHandler loadBalanceHandler = new LoadBalancerHandler(loadBalancer);

        rateLimitHandler.setNext(loadBalanceHandler);
        requestChain = rateLimitHandler;
    }

    @Override
    public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException,
            IOException {
        requestChain.handleRequest(req, res);
    }


}