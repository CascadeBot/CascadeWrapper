package com.cascadebot.cascadewrapper.sockets;

import com.cascadebot.cascadewrapper.JsonBuilder;
import com.cascadebot.cascadewrapper.Operation;
import com.cascadebot.cascadewrapper.Util;
import com.cascadebot.cascadewrapper.Wrapper;
import com.cascadebot.cascadewrapper.runnables.OperationRunnable;
import com.cascadebot.shared.OpCodes;
import com.cascadebot.shared.SecurityLevel;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

public class WrapperSocketServer extends WebSocketServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WrapperSocketServer.class);

    private static WrapperSocketServer instance;

    public WrapperSocketServer() {
        super();
        instance = this;
    }

    public WrapperSocketServer(InetSocketAddress address) {
        super(address);
        instance = this;
    }

    public static Set<SessionInfo> authenticatedUsers = new CopyOnWriteArraySet<>();
    public static Map<String, WebSocket> waitingAuth = new HashMap<>();
    private Set<WebSocket> connections = new HashSet<>();

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conn.setAttachment(new SessionInfo());
        System.out.println("new connection to " + conn.getRemoteSocketAddress());
        connections.add(conn);
        conn.send(new Packet(OpCodes.CONNECTED, new JsonBuilder().add("sessionid", ((SessionInfo) conn.getAttachment()).getUuid().toString())).toJSON());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        authenticatedUsers.remove(conn.getAttachment());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        SessionInfo sessionInfo = conn.getAttachment();
        sessionInfo.getRateLimiter().acquire(1);
        Packet packet = Packet.fromJSON(message);
        if (packet == null) { // If the received packet is invalid
            LOGGER.warn("Received invalid json");
            sendError(conn, "Invalid JSON!");
            return;
        }
        if (authenticatedUsers.contains(sessionInfo)) {
            switch (packet.getOpCode()) {
                case OpCodes.WRAPPER_OPERATION:
                    if (packet.getData().has("operation")) {
                        String operation = packet.getData().get("operation").getAsString();
                        Operation o = Util.getSafeEnum(Operation.class, operation);
                        if (o != null) {
                            if (!sessionInfo.getSecurityLevel().isAuthorised(o.getRequiredLevel())) {
                                return;
                            }
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
            if (packet.getOpCode() == OpCodes.AUTHORISE) { //TODO send errors
                if (packet.getData().has("user")) {
                    JsonObject user = packet.getData().get("user").getAsJsonObject();
                    verifyUser(user, conn);
                } else {
                    sendError(conn, "Must specify user info to authenticate");
                }
            } else {
                sendError(conn, "Not authorized!");
            }
        }
    }

    public void verifyUser(JsonObject user, WebSocket conn) {
        String id = user.get("id").getAsString();
        String hmac = user.get("hmac").getAsString();
        if (!Wrapper.getInstance().auth.verifyEncrypt(id, hmac)) {
            return;
        }
        Request request = new Request.Builder()
                .url("https://discordapp.com/api/guilds/" + Wrapper.getInstance().getGuild() + "/members/" + id)
                .get()
                .addHeader("Authorization", "Bot " + Wrapper.getInstance().botToken)
                .build();

        String res;

        try {
            Response response = Wrapper.getInstance().httpClient.newCall(request).execute();
            if (response.body() == null) return;
            res = response.body().string();
        } catch (IOException e) {
            sendError(conn, "Error validating with discord");
            return;
        }

        JsonObject jsonObject = new JsonParser().parse(res).getAsJsonObject();
        if (jsonObject.has("code")) return;
        JsonArray roles = jsonObject.getAsJsonArray("roles");

        Process process = OperationRunnable.getInstance().getManager().getProcess();
        if (process == null || !process.isAlive()) {
            if (roles.contains(new JsonPrimitive(Wrapper.getInstance().role))) {
                SessionInfo info = conn.getAttachment();
                info.setSecurityLevel("OWNER");
                conn.setAttachment(info);
                authenticatedUsers.add(info);
                LOGGER.info("Authorised user '" + getUserFromJson(jsonObject.getAsJsonObject("user")) + "' with address: " + conn.getRemoteSocketAddress().toString() + " and session ID: " + ((SessionInfo) conn.getAttachment()).getUuid());
                JsonBuilder operationJson = new JsonBuilder();
                operationJson.add("authorized", true);
                operationJson.add("sessionid", ((SessionInfo)conn.getAttachment()).getUuid().toString());
                conn.send(new Packet(OpCodes.AUTHORISE, operationJson).toJSON()); //TODO make method user authenticated
            } else {
                sendError(conn, "User is not authorized to do this!");
            }

            return;
        }

        waitingAuth.put(id, conn);

        String roleString = Wrapper.GSON.toJson(roles);

        roleString = roleString.replace("\n", "").replaceAll("\"","").replaceAll("\\s", "");
        roleString = roleString.substring(1, roleString.length() - 2);

        PrintWriter writer = new PrintWriter(process.getOutputStream());
        writer.println(SharedConstants.BOT_OP_PREFIX + " user " + id + " " + hmac + " " + roleString);
        writer.flush(); //Bot will now authenticate us

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (waitingAuth.containsKey(id)) {
                    waitingAuth.remove(id);
                    sendError(conn, "Auth timed out!");
                }
            }
        }, TimeUnit.SECONDS.toMillis(10));
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
                OpCodes.ERROR, new JsonBuilder().add("error", error)
        ).toJSON());
    }

    @Override
    public Set<WebSocket> getConnections() {
        return connections;
    }

    public void sendToAll(String data) {
        sendToAll(data, SecurityLevel.STAFF);
    }

    public void sendToAll(String data, SecurityLevel level) {
        for(WebSocket conn : connections) {
            SecurityLevel connSecurity = ((SessionInfo)conn.getAttachment()).getSecurityLevel();
            if(connSecurity != null) {
                if(connSecurity.isAuthorised(level)) {
                    conn.send(new Packet(5, data).toJSON());
                }
            }
        }
    }

    public static WrapperSocketServer getInstance() {
        return instance;
    }
}
