package com.cascadebot.cascadewrapper;

import com.google.gson.JsonPrimitive;

public class OperationJsonObject {

    com.google.gson.JsonObject object;

    public OperationJsonObject() {
        object = new com.google.gson.JsonObject();
    }

    public OperationJsonObject add(String key, String value) {
        object.add(key, new JsonPrimitive(value));
        return this;
    }

    public com.google.gson.JsonObject build() {
        return object;
    }

}
