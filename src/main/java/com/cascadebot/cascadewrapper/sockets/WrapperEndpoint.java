package com.cascadebot.cascadewrapper.sockets;

import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint(value = "/", decoders = PacketDecoder.class, encoders = PacketEncoder.class)
public class WrapperEndpoint  {

    private Session session;
    private static Set<WrapperEndpoint> wrapperEndpoints = new CopyOnWriteArraySet<>();
    private static Set<String> authenticatedUsers = new HashSet<>();

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        wrapperEndpoints.add(this);
    }

    @OnMessage
    public void onMessage(Session session, Packet packet) {
        if (authenticatedUsers.contains(session.getId())) {

        } else {
            if (packet.getOpCode() == 1 && packet.getData().has("token")) {
                // Process token and authenticate
                authenticatedUsers.add(session.getId());
            }
        }
    }

}
