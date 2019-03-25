package org.cascadebot.cascadewrapper.readers;

import org.cascadebot.cascadewrapper.Wrapper;
import org.cascadebot.cascadewrapper.process.CommandHandler;
import org.cascadebot.cascadewrapper.sockets.WrapperSocketServer;
import org.cascadebot.shared.OpCodes;
import org.cascadebot.shared.Regex;
import org.cascadebot.shared.SharedConstants;
import redis.clients.jedis.Jedis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ConsoleReader implements Runnable {

    private final InputStream inputStream;
    private final CommandHandler handler;

    public ConsoleReader(InputStream inputStream, CommandHandler handler) {
       this.inputStream = inputStream;
       this.handler = handler;
    }

    @Override
    public void run() {
        Jedis jedis = Wrapper.getInstance().jedis.getResource();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
            while ((line = reader.readLine()) != null && !Thread.interrupted()) {
                Wrapper.logger.info("[Bot] " + line);
                if(line.startsWith(SharedConstants.WRAPPER_OP_PREFIX)) {
                    Wrapper.logger.info("Received command from bot: " + line);
                    line = Regex.MULTISPACE_REGEX.matcher(line).replaceAll(" ");
                    handler.handleCommand(line.split(" "));
                } else {
                    WrapperSocketServer.getInstance().sendToAll(OpCodes.LOG, line);
                    jedis.set(String.valueOf(System.currentTimeMillis()), line);
                }
            }
        } catch (IOException e) {
            Wrapper.logger.error("Error reading process log", e);
        }
        jedis.close();
    }
}
