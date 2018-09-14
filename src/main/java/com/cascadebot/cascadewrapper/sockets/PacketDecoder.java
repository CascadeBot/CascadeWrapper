package com.cascadebot.cascadewrapper.sockets;

import com.cascadebot.cascadewrapper.Wrapper;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

public class PacketDecoder implements Decoder.Text<Packet> {
    @Override
    public Packet decode(String s) throws DecodeException {
        return Wrapper.GSON.fromJson(s, Packet.class);
    }

    @Override
    public boolean willDecode(String s) {
        return s != null;
    }

    @Override
    public void init(EndpointConfig config) {
        // Ignore this?
    }

    @Override
    public void destroy() {
        // Maybe
    }
}
