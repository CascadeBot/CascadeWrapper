package com.cascadebot.cascadewrapper;

import com.cascadebot.shared.ExitCodes;

import java.util.HashMap;
import java.util.Map;

public enum Operation {

    NOOP,
    FORCE_STOP, // Force operations should never be able be called from the bot
    FORCE_RESTART,
    FORCE_UPDATE,
    WRAPPER_STOP;

}
