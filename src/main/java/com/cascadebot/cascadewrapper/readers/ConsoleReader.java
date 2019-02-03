package com.cascadebot.cascadewrapper.readers;

import com.cascadebot.cascadewrapper.Wrapper;
import com.cascadebot.cascadewrapper.process.CommandHandler;
import com.cascadebot.shared.Regex;
import com.cascadebot.shared.SharedConstants;

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
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
            while ((line = reader.readLine()) != null && !Thread.interrupted()) {
                Wrapper.logger.info("[Bot] " + line);
                if(line.startsWith(SharedConstants.WRAPPER_OP_PREFIX)) {
                    Wrapper.logger.info("Received command from bot: " + line);
                    line = Regex.MULTISPACE_REGEX.matcher(line).replaceAll(" ");
                    handler.handleCommand(line.split(" "));
                }
            }
        } catch (IOException e) {
            Wrapper.logger.error("Error reading process log", e);
        }
    }
}
