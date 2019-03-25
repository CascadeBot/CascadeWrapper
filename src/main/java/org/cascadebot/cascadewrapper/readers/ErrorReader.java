package org.cascadebot.cascadewrapper.readers;

import org.cascadebot.cascadewrapper.Wrapper;
import org.cascadebot.cascadewrapper.sockets.WrapperSocketServer;
import com.cascadebot.shared.OpCodes;
import redis.clients.jedis.Jedis;

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
        Jedis jedis = Wrapper.getInstance().jedis.getResource();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
            while ((line = reader.readLine()) != null && !Thread.interrupted()) {
                WrapperSocketServer.getInstance().sendToAll(OpCodes.ERROR_LOG, line);
                Wrapper.logger.error("Received uncaught error from bot: '" + line + "'");
                jedis.set(String.valueOf(System.currentTimeMillis()), line);
            }
        } catch (IOException e) {
            Wrapper.logger.error("Error reading process log", e);
        }
        jedis.close();
    }
}
