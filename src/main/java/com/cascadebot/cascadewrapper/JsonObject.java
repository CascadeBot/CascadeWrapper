package com.cascadebot.cascadewrapper;

import com.google.gson.JsonPrimitive;

public class JsonObject {

    com.google.gson.JsonObject object;

    public JsonObject() {
        object = new com.google.gson.JsonObject();
    }

    public JsonObject add(String key, String value) {
        object.add(key, new JsonPrimitive(value));
        return this;
    }

    public com.google.gson.JsonObject build() {
        return object;
    }

}
