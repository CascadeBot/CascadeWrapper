package org.cascadebot.cascadewrapper.runnables;

import org.cascadebot.cascadewrapper.Wrapper;

import java.io.IOException;

public class ShutdownRunnable implements Runnable {
    @Override
    public void run() {
        /*Wrapper.getInstance().jedis.close();
        Process proc = OperationRunnable.instance.manager.getProcess();
        if(proc != null) {
            Wrapper.logger.info("Shutting down bot with wrapper");
            proc.destroy();
        }
        try {
            Wrapper.getInstance().server.stopServer();
        } catch (IOException | InterruptedException e) {
            Wrapper.logger.warn("Problem shutting down socket server", e);
        }*/
    }
}
