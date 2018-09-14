package com.cascadebot.cascadewrapper;

import java.util.HashMap;
import java.util.Map;

public enum Operation {

    NOOP,
    STOP(20),
    FORCE_STOP, // Force operations should never be able be called from the bot
    FORCE_RESTART,
    FORCE_UPDATE,
    UPDATE(21),
    RESTART(22);

    public static final Map<Integer, Operation> EXIT_CODE_MAP = new HashMap<>();

    static {
        for (Operation o : values()) {
            if (o.exitCode != -1) {
                EXIT_CODE_MAP.put(o.exitCode, o);
            }
        }
    }

    private int exitCode;

    Operation() {
        this.exitCode = -1;
    }

    Operation(int exitCode) {
        this.exitCode = exitCode;
    }

}
