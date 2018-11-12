package com.cascadebot.cascadewrapper;

import com.cascadebot.shared.ExitCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ProcessManager implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ProcessManager.class);
    private final AtomicInteger ERROR_RESTART_COUNT = new AtomicInteger(0);
    private Process process;
    private AtomicReference<RunState> state;
    private String fileName;
    private String[] args;
    private Queue<Operation> operationQueue = new LinkedBlockingQueue<>();
    private long lastStartTime;

    public ProcessManager(String filename, String[] args) {
        this.fileName = filename;
        this.args = args;
    }

    public Process start() {
        logger.info("Attempting to start process with filename: {}", fileName);
        try {
            if (Files.exists(Path.of(fileName))) {
                List<String> program = Arrays.asList("java", "-jar", fileName);
                program.addAll(Arrays.asList(args));
                ProcessBuilder builder = new ProcessBuilder(program);
                process = builder.start();
                logger.info("Successfully started process with filename: {}", fileName);
                state.set(RunState.STARTING);
                lastStartTime = System.currentTimeMillis();
                return process;
            } else {
                logger.error("The file {} does not exist, cannot start!", fileName);
                return null;
            }
        } catch (IOException e) {
            logger.error("There was an exception when starting!", e);
            return null;
        }
    }

    @Override
    public void run() {
        try {
            while (!Wrapper.SHUTDOWN.get() && process != null && process.isAlive()) {

                int exitCode = process.waitFor();
                state.set(RunState.STOPPED);

                if (exitCode == ExitCodes.STOP) {
                    ERROR_RESTART_COUNT.set(0);
                    logger.info("Stopping as requested by process.");
                } else if (exitCode == ExitCodes.RESTART) {
                    ERROR_RESTART_COUNT.set(0);
                    logger.info("Restarting after 2s as requested by process.");
                    Thread.sleep(2000); // Backoff
                    start();
                } else if (exitCode == ExitCodes.UPDATE) {
                    ERROR_RESTART_COUNT.set(0);
                    logger.info("Process has requested update. Will attempt to update and then restart on success.");
                    if (handleUpdate()) {
                        logger.info("Restarting process!");
                        start();
                    }
                } else if (exitCode == ExitCodes.ERROR_STOP_RESTART) {
                    if (ERROR_RESTART_COUNT.getAndIncrement() >= 3) {
                        // Log 3 failed restarts

                    } else {
                        Thread.sleep(5000 * ERROR_RESTART_COUNT.get());
                        start();
                    }

                } else if (exitCode == ExitCodes.ERROR_STOP_NO_RESTART) {
                } else {
                    logger.warn("Process executed with unknown exit code: {}", exitCode);
                    if ((System.currentTimeMillis() - lastStartTime) >= 5000) {
                        logger.info("Restarting process as its execution time was > 5s!");
                        Thread.sleep(5000);
                        start();
                        continue;
                    } else {
                        logger.info("Stopping process as it exited too quickly!");
                    }
                }
            }

            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private boolean handleUpdate() {

        return false;
    }

}
