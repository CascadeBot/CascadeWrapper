package com.cascadebot.cascadewrapper;

import com.cascadebot.cascadewrapper.sockets.WrapperSocketServer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class Wrapper {

    public static final Gson GSON = new GsonBuilder().create();
    private static final Logger logger = LoggerFactory.getLogger(Wrapper.class);


    public static void main(String[] args) {
        WrapperSocketServer server = new WrapperSocketServer(new InetSocketAddress("localhost",8080));
        server.start();
    }

}
