package com.chatflow.server;

import com.google.gson.*;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;

public class ChatServer extends WebSocketServer {
    private static final Gson gson = new Gson();

    public ChatServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Closed: " + conn.getRemoteSocketAddress() + " Reason: " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();

            // ✅ Validate message fields
            String validationError = validateMessage(json);
            if (validationError != null) {
                conn.send("{\"error\": \"" + validationError + "\"}");
                return;
            }

            // Add server timestamp + status
            json.addProperty("serverTimestamp", Instant.now().toString());
            json.addProperty("status", "OK");

            // Echo back
            conn.send(gson.toJson(json));
        } catch (Exception e) {
            conn.send("{\"error\": \"Invalid JSON\"}");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("🚀 Chat server started on port " + getPort());
    }

    // Validation logic
    private String validateMessage(JsonObject json) {
        try {
            int userId = json.get("userId").getAsInt();
            String username = json.get("username").getAsString();
            String msg = json.get("message").getAsString();
            String timestamp = json.get("timestamp").getAsString();
            String type = json.get("messageType").getAsString();

            if (userId < 1 || userId > 100000) return "Invalid userId";
            if (!username.matches("^[a-zA-Z0-9]{3,20}$")) return "Invalid username";
            if (msg.length() < 1 || msg.length() > 500) return "Invalid message length";
            Instant.parse(timestamp); // throws if invalid
            if (!type.matches("TEXT|JOIN|LEAVE")) return "Invalid messageType";
        } catch (Exception e) {
            return "Validation failed: " + e.getMessage();
        }
        return null;
    }

    // Main method
    public static void main(String[] args) {
        int port = 9090; // Default port
        ChatServer server = new ChatServer(port);
        server.start();
    }
}
