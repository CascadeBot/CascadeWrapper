package com.cascadebot.cascadewrapper.sockets;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class SessionInfo {

    private UUID uuid;

    public SessionInfo() {
        this.uuid = UUID.randomUUID();
    }

    public UUID getUuid() {
        return uuid;
    }
}
