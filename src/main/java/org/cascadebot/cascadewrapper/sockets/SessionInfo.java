package org.cascadebot.cascadewrapper.sockets;

import org.cascadebot.cascadewrapper.Util;
import com.cascadebot.shared.SecurityLevel;
import com.google.common.util.concurrent.RateLimiter;

import java.util.UUID;

public class SessionInfo {

    private UUID uuid;
    private RateLimiter rateLimiter;
    private SecurityLevel securityLevel = null;

    public SessionInfo() {
        this.uuid = UUID.randomUUID();
        rateLimiter = RateLimiter.create(10); // 10 Operations a second
    }

    public void setSecurityLevel(String securityLevel) {
        this.securityLevel = Util.getSafeEnum(SecurityLevel.class, securityLevel);
    }

    public UUID getUuid() {
        return uuid;
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }
}
