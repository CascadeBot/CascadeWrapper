package com.cascadebot.cascadewrapper;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class JsonBuilder {

    JsonObject object;

    public JsonBuilder() {
        object = new JsonObject();
    }

    public JsonBuilder add(String key, String value) {
        object.add(key, new JsonPrimitive(value));
        return this;
    }

    public JsonBuilder add(String key, boolean value) {
        object.add(key, new JsonPrimitive(value));
        return this;
    }

    public com.google.gson.JsonObject build() {
        return object;
    }

}
