package com.cascadebot.cascadewrapper.sockets;

import com.cascadebot.cascadewrapper.Wrapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class Packet {

    private final int opCode;
    private final JsonObject data;

    public Packet(int opCode, JsonObject data) {
        this.opCode = opCode;
        this.data = data;
    }

    public int getOpCode() {
        return opCode;
    }

    public JsonObject getData() {
        return data;
    }

    public String toJSON() {
        return Wrapper.GSON.toJson(this);
    }

    public static Packet fromJSON(String s) {
        try {
            return Wrapper.GSON.fromJson(s, Packet.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }



}
