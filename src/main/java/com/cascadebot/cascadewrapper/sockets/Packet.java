package com.cascadebot.cascadewrapper.sockets;

import com.cascadebot.cascadewrapper.Wrapper;
import org.json.JSONObject;

public class Packet {

    private final int opCode;
    private final JSONObject data;

    public Packet(int opCode, JSONObject data) {
        this.opCode = opCode;
        this.data = data;
    }

    public int getOpCode() {
        return opCode;
    }

    public JSONObject getData() {
        return data;
    }

}
