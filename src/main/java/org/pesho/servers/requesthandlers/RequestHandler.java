package org.pesho.servers.requesthandlers;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface RequestHandler {
    void setNext(RequestHandler handler);
    void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException;
}
