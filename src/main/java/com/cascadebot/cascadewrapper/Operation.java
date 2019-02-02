package com.cascadebot.cascadewrapper;

import com.cascadebot.shared.SecurityLevel;

public enum Operation {

    START(SecurityLevel.DEVELOPER),
    STOP(SecurityLevel.OWNER),
    RESTART(SecurityLevel.OWNER),
    UPDATE(SecurityLevel.OWNER),
    FORCE_STOP(SecurityLevel.OWNER), // Force operations should never be able be called from the bot
    FORCE_RESTART(SecurityLevel.OWNER),
    FORCE_UPDATE(SecurityLevel.OWNER),
    WRAPPER_STOP(SecurityLevel.OWNER),
    STOPPED_BY_WRAPPER(null);

    private int buildNumber = -1;

    SecurityLevel requiredLevel;

    Operation(SecurityLevel requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    public Operation setBuildNumber(int build) {
        // This is just a dev stupidity check
        if (this != Operation.UPDATE || this != Operation.FORCE_UPDATE) return this;
        this.buildNumber = build;
        return this;
    }

    public int getBuildNumber() {
        return buildNumber;
    }

    public SecurityLevel getRequiredLevel() {
        return requiredLevel;
    }

}
