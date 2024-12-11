package org.pesho.servers.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class InternalServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(final HttpServletRequest req,
                         final HttpServletResponse res) throws ServletException, IOException {
        res.getWriter()
                .append("Internal servlet, operational!");
    }
}
