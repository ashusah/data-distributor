import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.util.Random;

public class SimpleSignalServer {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(5000), 0);
        Random random = new Random();

        server.createContext("/create-signal/write-signal", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                int eventId = 100000 + random.nextInt(900000);
                String response = "{\"ceh_event_id\": " + eventId + "}";

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Server running on port 5000");
    }
}