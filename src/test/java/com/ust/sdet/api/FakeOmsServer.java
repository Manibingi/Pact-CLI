package com.ust.sdet.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;

public class FakeOmsServer {

    static HttpServer server;

    public static void start() throws Exception {

        server = HttpServer.create(new InetSocketAddress(4010), 0);
        server.createContext(
                "/customers/C100/points",
                e -> send(e, 200,
                        """
                        {
                          "customerId":"C100",
                          "points":2500
                        }
                        """
                )
        );

        server.createContext("/shipment/TRK123",
                e -> send(e, 200,
                        """
                        {
                          "trackingId":"TRK123",
                          "deliveryStatus":"IN_TRANSIT"
                        }
                        """
                )
        );

        server.createContext(
                "/payments/P900", e -> send(e, 200,
                        """
                        {
                          "paymentId":"P900",
                          "status":"SUCCESS"
                        }
                        """
                )
        );

        server.start();

        System.out.println("OMS Fake Server Started");
    }

    static void send(HttpExchange exchange, int status, String body) {
        try {
            byte[] response = body.getBytes();
            exchange
                    .getResponseHeaders()
                    .add(
                            "Content-Type",
                            "application/json"
                    );

            exchange.sendResponseHeaders(status, response.length);

            try (OutputStream os = exchange.getResponseBody()) {

                os.write(response);
            }
        }

        catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

}