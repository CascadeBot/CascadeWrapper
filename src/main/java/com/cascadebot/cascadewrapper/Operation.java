package com.cascadebot.cascadewrapper;

public enum Operation {

    NOOP,
    START,
    STOP,
    RESTART,
    UPDATE,
    FORCE_STOP, // Force operations should never be able be called from the bot
    FORCE_RESTART,
    FORCE_UPDATE,
    WRAPPER_STOP,
    STOPPED_BY_WRAPPER;

    private int buildNumber = -1;

    public Operation setBuildNumber(int build) {
        // This is just a dev stupidity check
        if (this != Operation.UPDATE || this != Operation.FORCE_UPDATE) return this;
        this.buildNumber = build;
        return this;
    }

    public int getBuildNumber() {
        return buildNumber;
    }

}
