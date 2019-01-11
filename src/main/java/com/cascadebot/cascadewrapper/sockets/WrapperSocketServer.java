package com.cascadebot.cascadewrapper.sockets;

import com.cascadebot.cascadewrapper.JsonObject;
import com.cascadebot.cascadewrapper.Operation;
import com.cascadebot.cascadewrapper.Util;
import com.cascadebot.cascadewrapper.Wrapper;
import com.cascadebot.cascadewrapper.runnables.OperationRunnable;
import com.cascadebot.shared.OpCodes;
import com.cascadebot.shared.utils.ThreadPoolExecutorLogged;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

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
            LOGGER.warn("Received invalid json");
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
                            OperationRunnable.queueOperation(o);
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
                if(packet.getData().get("token").getAsString().equals(Wrapper.getInstance().token)) {
                    authenticatedUsers.add(conn.getAttachment()); // Authorises this connection
                    LOGGER.info("Authorised user with address: " + conn.getRemoteSocketAddress().toString() + " and session ID: " + ((SessionInfo) conn.getAttachment()).getUuid());
                } else {
                    sendError(conn, "Invalid token");
                }
            } else {
                sendError(conn, "Must specify token to authenticate");
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
