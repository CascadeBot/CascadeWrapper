package com.cascadebot.cascadewrapper.sockets;

import com.cascadebot.cascadewrapper.OperationJsonObject;
import com.cascadebot.cascadewrapper.Operation;
import com.cascadebot.cascadewrapper.Util;
import com.cascadebot.cascadewrapper.Wrapper;
import com.cascadebot.cascadewrapper.runnables.OperationRunnable;
import com.cascadebot.shared.OpCodes;
import com.cascadebot.shared.SharedConstants;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import okhttp3.Request;
import okhttp3.Response;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class WrapperSocketServer extends WebSocketServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WrapperSocketServer.class);

    private static WrapperSocketServer instance;

    public WrapperSocketServer() {
        super();
    }

    public WrapperSocketServer(InetSocketAddress address) {
        super(address);
    }

    public static Set<SessionInfo> authenticatedUsers = new CopyOnWriteArraySet<>();
    public static Map<String, SessionInfo> waitingAuth = new HashMap<>();
    private Set<WebSocket> connections = new HashSet<>();

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conn.setAttachment(new SessionInfo());
        System.out.println("new connection to " + conn.getRemoteSocketAddress());
        connections.add(conn);
        conn.send(new Packet(OpCodes.CONNECTED, new OperationJsonObject().add("sessionid", ((SessionInfo) conn.getAttachment()).getUuid().toString()).build()).toJSON());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
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
                            // If no build number is specified in the packet, the bot updates to the latest successful build
                            if ((o == Operation.UPDATE || o == Operation.FORCE_UPDATE) && packet.getData().has("build")) {
                                o.setBuildNumber(packet.getData().get("build").getAsInt());
                            }
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
            if (packet.getOpCode() == OpCodes.AUTHORISE && packet.getData().has("user")) { //TODO send error
                JsonObject user = packet.getData().get("user").getAsJsonObject();
                String id = user.get("id").getAsString();
                String hmac = user.get("hmac").getAsString();
                if(Wrapper.getInstance().auth.verifyEncrypt(id, hmac)) {

                    return;
                }
                Process process = OperationRunnable.getInstance().getManager().getProcess();
                if (process == null || !process.isAlive()) {
                    Request request = new Request.Builder()
                            .url("https://discordapp.com/api/guilds/488394590458478602/members/215644829969809421")
                            .get()
                            .addHeader("Authorization", "Bot " + Wrapper.getInstance().botToken)
                            .build();

                    try {
                        Response response = Wrapper.getInstance().httpClient.newCall(request).execute();
                        if(response.body() == null) return;
                        String res = response.body().string();
                        JsonObject jsonObject = new JsonParser().parse(res).getAsJsonObject();
                        if(jsonObject.has("code")) return;
                        JsonArray roles = jsonObject.getAsJsonArray("roles");
                        if(roles.contains(new JsonPrimitive(Wrapper.getInstance().role))) {
                            authenticatedUsers.add(conn.getAttachment());
                            LOGGER.info("Authorised user '" + getUserFromJson(jsonObject.getAsJsonObject("user")) + "' with address: " + conn.getRemoteSocketAddress().toString() + " and session ID: " + ((SessionInfo) conn.getAttachment()).getUuid());
                        }

                    } catch (IOException e) {
                        Wrapper.logger.error("Error checking user from discord", e);
                        return;
                    }

                    return;
                }

                waitingAuth.add(id);

                PrintWriter writer = new PrintWriter(process.getOutputStream());
                writer.println(SharedConstants.BOT_OP_PREFIX + " user " + id + " " + hmac);
                writer.flush(); //Bot will now authenticate us
            } else {
                sendError(conn, "Must specify token to authenticate");
            }
        }
    }

    private String getUserFromJson(JsonObject object) {
        return object.get("username").getAsString() + "#" + object.get("discriminator").getAsString();
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

    public void stopServer() throws IOException, InterruptedException {
        for (WebSocket conn : connections) {
            conn.close();
        }
        this.stop();
    }

    public void sendError(WebSocket conn, String error) {
        conn.send(new Packet(
                OpCodes.ERROR, new OperationJsonObject().add("error", error).build()
        ).toJSON());
    }

    @Override
    public Set<WebSocket> getConnections() {
        return connections;
    }

    public static WrapperSocketServer getInstance() {
        return instance;
    }
}
