package org.pesho.servers.servlets;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.pesho.servers.loadbalancing.LoadBalancer;
import org.pesho.servers.loadbalancing.RoundRobinBalancer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class ResenderServlet extends HttpServlet {

    private static final List<String> RESTRICTED_HEADERS = Arrays.asList(
            "connection",
            "content-length",
            "expect",
            "host",
            "upgrade",
            "connection-security",
            "transfer-encoding",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "upgrade"
    );

    private final HttpClient httpClient;
    private LoadBalancer loadBalancer;

    public ResenderServlet() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        this.loadBalancer = (LoadBalancer) config.getServletContext().getAttribute("loadBalancer");
        super.init(config);
    }

    @Override
    public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException,
            IOException {
        forwardRequest(loadBalancer.nextServer(), req, res);
    }

    private void forwardRequest(String targetServer, HttpServletRequest req,
                                HttpServletResponse res)
            throws IOException {
        try {
            // Build target URL - make uri later when loadbalancing
            String targetUrl = buildTargetUrl(req, targetServer);

            // Create HTTP request
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(new URI(targetUrl))
                    .method(req.getMethod(), getRequestBody(req));

            // Copy original headers
            Collections.list(req.getHeaderNames()).forEach(headerName -> {
                if (isNotRestrictedHeader(headerName)) {
                    requestBuilder.header(headerName, req.getHeader(headerName));
                }
            });

            // Send request and get response
            var response = httpClient.send(requestBuilder.build(),
                    BodyHandlers.ofByteArray());

            // Copy response status
            res.setStatus(response.statusCode());

            // Copy response headers
            response.headers().map().forEach((name, values) -> {
                if (isNotRestrictedHeader(name)) {
                    values.forEach(value -> res.addHeader(name, value));
                }
            });

            // Copy response body
            res.getOutputStream().write(response.body());

        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            res.getWriter().write("Error forwarding request: " + e.getMessage());
        }
    }

    private boolean isNotRestrictedHeader(String headerName) {
        return !RESTRICTED_HEADERS.contains(headerName.toLowerCase());
    }

    private static BodyPublisher getRequestBody(HttpServletRequest req) throws IOException {
        if (req.getContentLength() == 0) {
            return HttpRequest.BodyPublishers.noBody();
        }

        byte[] body = req.getInputStream().readAllBytes();
        return HttpRequest.BodyPublishers.ofByteArray(body);
    }

    private static String buildTargetUrl(HttpServletRequest req, String targetServer) {
        String originalPath = req.getRequestURI();
        String queryString = req.getQueryString();
        return "http://" + targetServer + originalPath +
               (queryString != null ? "?" + queryString : "");
    }
}