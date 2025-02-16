package com.mfahproj.webapp.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;

import com.mfahproj.webapp.Database;
import com.mfahproj.webapp.Session;
import com.mfahproj.webapp.Utils;
import com.mfahproj.webapp.models.Museum;
import com.mfahproj.webapp.models.Employee;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RegisterMuseumHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            get(exchange);
        } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            post(exchange);
        }
    }

    // Handles GET requests from the client.
    private void get(HttpExchange exchange) throws IOException {
        // Check if a session exists.
        String sessionId = Session.extractSessionId(exchange);
        Employee employee = Session.getEmployeeSession(sessionId);
        if (employee == null) {
            // No prior session, send to login page.
            exchange.getResponseHeaders().add("Location", "/login");
            exchange.sendResponseHeaders(302, -1);
            return;
        }

        // Show register form for an employee.
        String response = Utils.dynamicNavigator(exchange, "museum/register.html");

        exchange.sendResponseHeaders(200, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    // Handles POST requests from the client.
    private void post(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
        BufferedReader br = new BufferedReader(isr);
        String formData = br.readLine();

        // Parse the form data to create a new user.
        Map<String, String> form = Utils.parseForm(formData);
        Museum obj = RegisterMuseumHandler.createMuseum(form);

        String response;
        switch (Database.createMuseum(obj)) {
            case SUCCESS:
                // Artifact created
                exchange.getResponseHeaders().add("Location", "/success");
                exchange.sendResponseHeaders(302, -1);

                System.out.printf("%s created.\n", obj.getName());
                return;
            case DUPLICATE:
                // Duplicate museum detected, refresh the museum register page.
                System.out.printf("%s is a duplicate museum.\n", obj.getName());
                response = "<body>"
                        + "    <h4>Museum already exists, please try again.</h4>"
                        + "    <a href='/museum/register'>Register Museum</a>"
                        + "</body>";

                break;
            default:
                // Could not create Museum.
                System.out.printf("%s failed to create.\n", obj.getName());
                response = "An unknown error!";
        }

        // Send the response based on the error.
        exchange.sendResponseHeaders(200, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    // Creates a new Museum from the form data provided.
    private static Museum createMuseum(Map<String, String> form) {
        Museum obj = new Museum();

        obj.setName(form.get("Name"));
        obj.setAddress(form.get("Address"));
        obj.setTotalRevenue(Integer.parseInt(form.get("TotalRevenue")));
        obj.setOperationalCost(Integer.parseInt(form.get("OperationalCost")));

        return obj;
    }
}
