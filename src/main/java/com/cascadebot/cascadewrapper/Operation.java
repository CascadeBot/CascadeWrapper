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
    WRAPPER_STOP;

}
