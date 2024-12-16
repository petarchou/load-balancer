package org.pesho.servers.requesthandlers;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public abstract class BaseHandler implements RequestHandler {
    protected RequestHandler nextHandler;

    @Override
    public void setNext(RequestHandler handler) {
        this.nextHandler = handler;
    }

    protected void processNext(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        if (nextHandler != null) {
            nextHandler.handleRequest(request, response);
        }
    }
}
