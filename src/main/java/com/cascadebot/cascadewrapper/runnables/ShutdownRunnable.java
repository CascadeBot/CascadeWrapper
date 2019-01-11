package com.cascadebot.cascadewrapper.runnables;

import com.cascadebot.cascadewrapper.Wrapper;

public class ShutdownRunnable implements Runnable {
    @Override
    public void run() {
        Process proc = OperationRunnable.instance.manager.getProcess();
        if(proc != null) {
            Wrapper.logger.info("Shutting down bot with wrapper");
            proc.destroy();
        }
    }
}
