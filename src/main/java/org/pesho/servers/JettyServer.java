package org.pesho.servers;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.pesho.servers.servlets.InternalServlet;
import org.pesho.servers.servlets.ResenderServlet;

public class JettyServer {
    private static final String INTERNAL_PATH = "/loadbalancer";

    public void start() {
        Server server = new Server();
        try (ServerConnector c = new ServerConnector(server)) {

            c.setIdleTimeout(1000);
            c.setAcceptQueueSize(100);
            c.setPort(80);
            c.setHost("localhost");
            ServletContextHandler handler = new ServletContextHandler("", true, false);
            ServletHolder genericHolder = new ServletHolder(InternalServlet.class);
            ServletHolder resenderHolder = new ServletHolder(ResenderServlet.class);
            handler.addServlet(genericHolder, "%s/generic".formatted(INTERNAL_PATH));
            handler.addServlet(resenderHolder, "/*");
            server.setHandler(handler);
            server.addConnector(c);
            server.start();
            System.out.println("Server started");
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
