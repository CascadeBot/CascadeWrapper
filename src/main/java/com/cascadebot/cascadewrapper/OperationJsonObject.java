package com.cascadebot.cascadewrapper;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class OperationJsonObject {

    JsonObject object;

    public OperationJsonObject() {
        object = new JsonObject();
    }

    public OperationJsonObject add(String key, String value) {
        object.add(key, new JsonPrimitive(value));
        return this;
    }

    public OperationJsonObject add(String key, boolean value) {
        object.add(key, new JsonPrimitive(value));
        return this;
    }

    public com.google.gson.JsonObject build() {
        return object;
    }

}
