package com.cascadebot.cascadewrapper;

public class Util {

    public static <T extends Enum<T>> T getSafeEnum(Class<T> enumClass, String value) {
        try {
            return T.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
