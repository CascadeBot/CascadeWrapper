package org.cascadebot.cascadewrapper;

import org.cascadebot.shared.SharedConstants;

public class Util {

    public static <T extends Enum<T>> T getSafeEnum(Class<T> enumClass, String value) {
        try {
            return T.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String getBotCommand(String command, String[] args) {
        return SharedConstants.BOT_OP_PREFIX + " " + command + String.join(" ", args);
    }

}
