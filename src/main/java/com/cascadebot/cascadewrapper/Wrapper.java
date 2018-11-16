package com.cascadebot.cascadewrapper;

import com.cascadebot.cascadewrapper.sockets.WrapperSocketServer;
import com.cascadebot.shared.ExitCodes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class Wrapper {

    public static final Gson GSON = new GsonBuilder().create();
    public static final AtomicBoolean shutdown = new AtomicBoolean(false);
    private static final Logger logger = LoggerFactory.getLogger(Wrapper.class);
    private static Wrapper instance;

    private WrapperSocketServer server;

    public static void main(String[] args) {
        (instance = new Wrapper()).init();
    }

    public static Wrapper getInstance() {
        return instance;
    }

    private void init() {
        server = new WrapperSocketServer(new InetSocketAddress("localhost",8080));
        server.start();

        logger.error("hi");

    }
}
