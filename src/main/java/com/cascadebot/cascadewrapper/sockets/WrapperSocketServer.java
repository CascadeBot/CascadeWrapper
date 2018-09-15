package com.cascadebot.cascadewrapper.sockets;

import com.cascadebot.cascadewrapper.JsonObject;
import com.cascadebot.cascadewrapper.Operation;
import com.cascadebot.cascadewrapper.Util;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class WrapperSocketServer extends WebSocketServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WrapperSocketServer.class);

    public WrapperSocketServer() {
        super();
    }

    public WrapperSocketServer(InetSocketAddress address) {
        super(address);
    }

    public static Set<SessionInfo> authenticatedUsers = new CopyOnWriteArraySet<>();

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conn.setAttachment(new SessionInfo());
        System.out.println("new connection to " + conn.getRemoteSocketAddress());
        conn.send(new Packet(OpCodes.CONNECTED, new JsonObject().add("sessionid", ((SessionInfo) conn.getAttachment()).getUuid().toString()).build()).toJSON());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        authenticatedUsers.remove(conn.getAttachment());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        ((SessionInfo) conn.getAttachment()).getRateLimiter().acquire(1);
        Packet packet = Packet.fromJSON(message);
        if (packet == null) { // If the received packet is invalid
            sendError(conn, "Invalid JSON!");
            return;
        }
        if (authenticatedUsers.contains(conn.getAttachment())) {
            switch (packet.getOpCode()) {
                case OpCodes.WRAPPER_OPERATION:
                    if (packet.getData().has("operation")) {
                        String operation = packet.getData().get("operation").getAsString();
                        Operation o = Util.getSafeEnum(Operation.class, operation);
                        if (o != null) {
                            // Handle Operation
                            break;
                        } else {
                            sendError(conn, "Invalid operation!");
                            break;
                        }
                    }
                default:
            }
        } else {
            if (packet.getOpCode() == OpCodes.AUTHORISE && packet.getData().has("token")) {
                // Authorise Token
                authenticatedUsers.add(conn.getAttachment()); // Authorises this connection
                LOGGER.debug("Authorised user with address: " + conn + " and session ID: " + ((SessionInfo) conn.getAttachment()).getUuid());
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            sendError(conn, ex.toString());
        }
        LOGGER.error("Error in WebSocket Server", ex);
    }

    @Override
    public void onStart() {
        LOGGER.info("Wrapper Socket Server started and listening on port:" + this.getAddress().getPort());
    }

    private void sendError(WebSocket conn, String error) {
        conn.send(new Packet(
                OpCodes.ERROR, new JsonObject().add("error", error).build()
        ).toJSON());
    }

}
