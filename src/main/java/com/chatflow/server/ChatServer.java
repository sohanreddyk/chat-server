package com.chatflow.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.regex.Pattern;

public class ChatServer extends WebSocketServer {

    private static final Gson gson = new Gson();
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]{3,20}$");

    public ChatServer(int port) {
        super(new InetSocketAddress("0.0.0.0", port)); // ✅ bind to all interfaces
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("✅ New connection from " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("❌ Connection closed: " + conn.getRemoteSocketAddress() + " Reason: " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            if (isValidMessage(json)) {
                json.addProperty("serverTimestamp", Instant.now().toString());
                json.addProperty("status", "OK");
                conn.send(gson.toJson(json));
            } else {
                sendError(conn, "Invalid message format");
            }
        } catch (JsonSyntaxException e) {
            sendError(conn, "Malformed JSON");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("⚠️ Error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("🚀 Chat server started on port " + getPort());
    }

    private boolean isValidMessage(JsonObject json) {
        try {
            int userId = json.get("userId").getAsInt();
            if (userId < 1 || userId > 100000) return false;

            String username = json.get("username").getAsString();
            if (!USERNAME_PATTERN.matcher(username).matches()) return false;

            String message = json.get("message").getAsString();
            if (message.length() < 1 || message.length() > 500) return false;

            String timestamp = json.get("timestamp").getAsString();
            Instant.parse(timestamp); // throws if invalid

            String messageType = json.get("messageType").getAsString();
            if (!(messageType.equals("TEXT") || messageType.equals("JOIN") || messageType.equals("LEAVE"))) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void sendError(WebSocket conn, String errorMsg) {
        JsonObject error = new JsonObject();
        error.addProperty("status", "ERROR");
        error.addProperty("message", errorMsg);
        conn.send(gson.toJson(error));
    }

    // ✅ Health check server (on port 8080, bind to 0.0.0.0)
    public static void startHealthServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8080), 0);
        server.createContext("/health", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String response = "OK";
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        });
        new Thread(server::start).start();
        System.out.println("❤️  Health endpoint on port 8080 at /health");
    }

    public static void main(String[] args) throws Exception {
        int wsPort = 9090;
        ChatServer server = new ChatServer(wsPort);
        server.start();
        System.out.println("🚀 Chat server started on port " + wsPort);

        // Start HTTP health server
        startHealthServer();
    }
}
