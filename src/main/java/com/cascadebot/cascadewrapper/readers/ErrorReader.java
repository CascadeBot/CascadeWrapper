package com.cascadebot.cascadewrapper.readers;

import com.cascadebot.cascadewrapper.Wrapper;
import com.cascadebot.cascadewrapper.sockets.WrapperSocketServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ErrorReader implements Runnable {

    private final InputStream inputStream;

    public ErrorReader(InputStream errorStream) {
        inputStream = errorStream;
    }

    @Override
    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
            while ((line = reader.readLine()) != null && !Thread.interrupted()) {
                WrapperSocketServer.getInstance().sendToAll(line);
                Wrapper.logger.error("Received uncaught error from bot: '" + line + "'");
            }
        } catch (IOException e) {
            Wrapper.logger.error("Error reading process log", e);
        }
    }
}
