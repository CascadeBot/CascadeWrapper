package com.cascadebot.cascadewrapper.sockets;

import com.google.common.util.concurrent.RateLimiter;

import java.util.UUID;

public class SessionInfo {

    private UUID uuid;
    private RateLimiter rateLimiter;

    public SessionInfo() {
        this.uuid = UUID.randomUUID();
        rateLimiter = RateLimiter.create(10); // 10 Operations a second
    }

    public UUID getUuid() {
        return uuid;
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }
}
