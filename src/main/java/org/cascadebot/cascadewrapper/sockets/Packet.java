package org.cascadebot.cascadewrapper.sockets;

import org.cascadebot.cascadewrapper.JsonBuilder;
import org.cascadebot.cascadewrapper.Wrapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class Packet {

    private final int opCode;
    private final JsonObject data;

    public Packet(int opCode, JsonBuilder data) {
        this.opCode = opCode;
        this.data = data.build();
    }

    public Packet(int opCode, JsonObject object) {
        this.opCode = opCode;
        this.data = object;
    }

    public Packet(int opCode, String data) {
        this.opCode = opCode;
        this.data = new JsonBuilder().add("d", data).build();
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
