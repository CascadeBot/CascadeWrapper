package com.cascadebot.cascadewrapper;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProcessManager implements Runnable {

    private Process process;
    private String[] command;

    private Queue<Operation> operationQueue = new LinkedBlockingQueue<>();
    private static final AtomicBoolean shutdown = new AtomicBoolean(false);

    public ProcessManager(String[] command) {
        this.command = command;
    }

    public boolean start() {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            process = builder.start();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void run() {
        while (!shutdown.get()) {
            if (process != null && process.isAlive()) {
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleOperation(int exitCode) {
        handleOperation(Operation.EXIT_CODE_MAP.getOrDefault(exitCode, Operation.NOOP));
    }

    private boolean handleOperation(Operation operation)  {
        switch (operation) {
            case STOP:
                // REST API stop
                break;
            case FORCE_STOP:
                process.destroy();
                return true;
            case FORCE_RESTART:
                process.destroy();
                return start();
            case FORCE_UPDATE:
                process.destroy();
                // Update
                return start();
            case UPDATE:
                if (handleOperation(Operation.STOP)) {
                    // Handle Update
                }
                break;
            case RESTART:
                break;
            case NOOP:
                return true; // No Operation
        }
        return true;
    }

    public static void shutdown() {
        shutdown.getAndSet(true);
    }

}
