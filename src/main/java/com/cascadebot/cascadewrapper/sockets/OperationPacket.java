package com.cascadebot.cascadewrapper.sockets;

import com.cascadebot.cascadewrapper.Operation;
import org.json.JSONObject;

public class OperationPacket extends Packet {

    public OperationPacket(Operation operation) {
        super(1, );
    }

}
