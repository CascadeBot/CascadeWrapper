package com.cascadebot.cascadewrapper.sockets;

import com.cascadebot.cascadewrapper.Wrapper;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

public class PacketEncoder implements Encoder.Text<Packet> {


    @Override
    public String encode(Packet packet) throws EncodeException {
        return Wrapper.GSON.toJson(packet);
    }

    @Override
    public void init(EndpointConfig config) {
        // Ignore this?
    }

    @Override
    public void destroy() {
        // This too?
    }
}
