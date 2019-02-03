package com.cascadebot.cascadewrapper;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class JSONObject {

    JsonObject object;

    public JSONObject() {
        object = new JsonObject();
    }

    public JSONObject add(String key, String value) {
        object.add(key, new JsonPrimitive(value));
        return this;
    }

    public JSONObject add(String key, boolean value) {
        object.add(key, new JsonPrimitive(value));
        return this;
    }

    public com.google.gson.JsonObject build() {
        return object;
    }

}
