package com.cascadebot.cascadewrapper.runnables;

import com.cascadebot.cascadewrapper.Operation;
import com.cascadebot.cascadewrapper.RunState;
import com.cascadebot.cascadewrapper.Wrapper;
import com.cascadebot.cascadewrapper.utils.Downloader;
import com.cascadebot.shared.ExitCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ProcessManager implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessManager.class);
    private final AtomicInteger errorRestartCount = new AtomicInteger(0);
    private final String fileName;
    private final String[] args;

    private Process process;
    private AtomicReference<RunState> state;
    private long lastStartTime;

    private Thread processThread;

    private boolean restartTimer = false;

    public ProcessManager(String filename, String[] args) {
        this.fileName = filename;
        this.args = args;
    }

    public void start() {
        LOGGER.info("Attempting to start process with filename: {}", fileName);
        try {
            if (Files.exists(Path.of(fileName))) {
                List<String> program = Arrays.asList("java", "-jar", fileName);
                program.addAll(Arrays.asList(args));
                ProcessBuilder builder = new ProcessBuilder(program);
                builder.directory(new File(Wrapper.cascadeWorkingDir));
                process = builder.start();
                processThread = new Thread(this);
                processThread.start();
                LOGGER.info("Successfully started process with filename: {}", fileName);
                state.set(RunState.STARTING);
                lastStartTime = System.currentTimeMillis();
            } else {
                LOGGER.error("The file {} does not exist, cannot start!", fileName);
            }
        } catch (IOException e) {
            LOGGER.error("There was an exception when starting!", e);
        }
    }

    @Override
    public void run() {
        try {
            while (!Wrapper.shutdown.get() && process != null && process.isAlive()) {

                int exitCode = process.waitFor();
                state.set(RunState.STOPPED);

                if (exitCode == ExitCodes.STOP) {
                    errorRestartCount.set(0);
                    LOGGER.info("Stopping as requested by process.");
                } else if (exitCode == ExitCodes.RESTART) {
                    errorRestartCount.set(0);
                    LOGGER.info("Restarting after 2s as requested by process.");
                    Thread.sleep(2000); // Backoff
                    start();
                } else if (exitCode == ExitCodes.UPDATE) {
                    errorRestartCount.set(0);
                    LOGGER.info("Process has requested update. Will attempt to update and then restart on success.");
                    if (handleUpdate()) {
                        LOGGER.info("Restarting process!");
                        start();
                    }
                } else if (exitCode == ExitCodes.ERROR_STOP_RESTART) {
                    if (errorRestartCount.getAndIncrement() >= 3) {
                        // Log 3 failed restarts
                        LOGGER.error("Process restarted 3 times within 10 mins. We will stop restarting");
                    } else {
                        if(!restartTimer) {
                            restartTimer = true;
                            Timer timer = new Timer();
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    errorRestartCount.set(0);
                                    restartTimer = false;
                                }
                            }, TimeUnit.MINUTES.toMillis(10));
                        }
                        Thread.sleep(5000 * errorRestartCount.get());
                        start();
                    }

                } else if (exitCode == ExitCodes.ERROR_STOP_NO_RESTART) {
                    process.destroy();
                    LOGGER.error("Process stopped with error code ERROR_STOP_NO_RESTART. Please check the program logs to find the issue");
                } else {
                    LOGGER.warn("Process executed with unknown exit code: {}", exitCode);
                    if ((System.currentTimeMillis() - lastStartTime) >= 5000) {
                        LOGGER.info("Restarting process as its execution time was > 5s!");
                        Thread.sleep(5000);
                        start();
                    } else {
                        LOGGER.info("Stopping process as it exited too quickly!");
                    }
                }
            }

            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public boolean handleUpdate() {
        String url = "https://jenkins.weeryan17.com/job/Cascade/lastSuccessfulBuild/artifact/target/CascadeBot-jar-with-dependencies.jar";
        try {
            Downloader downloader = new Downloader(new URL(url), new File(Wrapper.cascadeWorkingDir, "CascadeBot.jar"));
            while (downloader.getStatus() == Downloader.DOWNLOADING) {

            }
            if(downloader.getStatus() == Downloader.COMPLETE) {
                OperationRunnable.queueOperation(Operation.RESTART);
                return true;
            } else {
                return false;
            }
        } catch (MalformedURLException e) {
            LOGGER.error("Invalid download url: " + url, e);
            return false;
        }
    }

    public synchronized Process getProcess() {
        return process;
    }

    public synchronized AtomicReference<RunState> getState() {
        return state;
    }

    public String getFileName() {
        return fileName;
    }

    public String[] getArgs() {
        return args;
    }

    public long getLastStartTime() {
        return lastStartTime;
    }
}
