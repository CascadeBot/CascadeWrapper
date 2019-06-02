package org.cascadebot.cascadewrapper.process;

import org.cascadebot.cascadewrapper.Operation;
import org.cascadebot.cascadewrapper.Util;
import org.cascadebot.cascadewrapper.Wrapper;
import org.cascadebot.cascadewrapper.readers.ConsoleReader;
import org.cascadebot.cascadewrapper.readers.ErrorReader;
import org.cascadebot.cascadewrapper.runnables.OperationRunnable;
import org.cascadebot.shared.ExitCodes;
import org.cascadebot.shared.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ProcessManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessManager.class);


    private final AtomicInteger errorRestartCount = new AtomicInteger(0);
    private final String fileName;
    private final String[] args;

    private Process process;
    private AtomicReference<RunState> state = new AtomicReference<>();
    private long lastStartTime;

    private ProcessStats processStats;

    private Thread consoleReaderThread;
    private Thread errorReaderThread;
    private Thread processManagerThread;

    private boolean restartTimer = false;

    private CommandHandler handler;

    private boolean forceStooped = false;

    private Version currentVersion;

    public ProcessManager(String filename, String[] args) {
        File versionFile = new File(Wrapper.cascadeWorkingDir, "version.txt");
        try {
            currentVersion = getVersionFromStream(new FileInputStream(versionFile));
        } catch (FileNotFoundException e) {
            getLOGGER().error("Error reading version file", e);
        }
        this.fileName = filename;
        this.args = args;
        state.set(RunState.STOPPED);
        //handler = new CommandHandler(this);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if(consoleReaderThread != null) {
                consoleReaderThread.interrupt();
            }
        }));
        processStats = new ProcessStats(this);
    }

    public void start() {
        (processManagerThread = new Thread(this::startProcess)).start();
    }

    public void interrupt() {
        processManagerThread.interrupt();
    }

    private void startProcess() {
        LOGGER.info("Attempting to start process with filename: {}", fileName);
        state.set(RunState.STARTING);
        try {
            if (Files.exists(new File(Wrapper.cascadeWorkingDir, fileName).toPath())) {
                List<String> program = Arrays.asList("java", "-jar", fileName);
                program.addAll(Arrays.asList(args));
                ProcessBuilder builder = new ProcessBuilder(program);
                builder.directory(new File(Wrapper.cascadeWorkingDir));
                process = builder.start();
                state.set(RunState.STARTED);
                LOGGER.info("Successfully started process with filename: {}", fileName);
                lastStartTime = System.currentTimeMillis();
                run();
            } else {
                state.set(RunState.STOPPED);
                LOGGER.error("The file {} does not exist, cannot start!", fileName);
            }
        } catch (IOException e) {
            LOGGER.error("There was an exception when starting!", e);
        }
    }

    public void run() {
        try {
            (consoleReaderThread = new Thread(new ConsoleReader(process.getInputStream(), handler))).start();
            (errorReaderThread = new Thread(new ErrorReader(process.getErrorStream()))).start();

            int exitCode = process.waitFor();
            consoleReaderThread.interrupt(); // Once the process has ended we need to stop this
            errorReaderThread.interrupt();

            state.set(RunState.STOPPED);

            if (exitCode == ExitCodes.STOP) {
                errorRestartCount.set(0);
                LOGGER.info("Stopping as requested by process.");
            } else if (exitCode == ExitCodes.RESTART) {
                errorRestartCount.set(0);
                LOGGER.info("Restarting after 2s as requested by process.");
                Thread.sleep(2000); // Backoff
                startProcess();
            } else if (exitCode == ExitCodes.UPDATE) {
                errorRestartCount.set(0);
                LOGGER.info("Process has requested update. Will attempt to update and then restart on success.");
                if (handleUpdate(-1)) {
                    LOGGER.info("Restarting process!");
                    startProcess();
                }
            } else if (exitCode == ExitCodes.ERROR_STOP_RESTART) {
                if (errorRestartCount.getAndIncrement() >= 3) {
                    // Log 3 failed restarts
                    LOGGER.error("Process restarted 3 times within 10 mins. We will stop restarting");
                } else {
                    if (!restartTimer) {
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
                    startProcess();
                }

            } else if (exitCode == ExitCodes.ERROR_STOP_NO_RESTART) {
                LOGGER.error("Process stopped with error code ERROR_STOP_NO_RESTART. Please check the program logs to find the issue");
            } else if (exitCode == ExitCodes.STOP_WRAPPER) {
                LOGGER.info("Process exited, now stopping wrapper");
                Wrapper.shutdown.set(true);
                System.exit(0);
            } else if (exitCode == ExitCodes.STOPPED_BY_WRAPPER) {
                LOGGER.info("Process stopped by wrapper");
            } else if (exitCode == 1) {
                if(forceStooped) {
                    forceStooped = false;
                    LOGGER.info("Process force stooped by wrapper");
                } else {
                    unknownExitCode(1);
                }
            } else {
                unknownExitCode(exitCode);
            }

            Thread.sleep(1000);
        } catch (InterruptedException e) {
            LOGGER.error("Process waiting has been interrupted!", e);
        }

    }

    private void unknownExitCode(int exitCode) throws InterruptedException {
        LOGGER.warn("Process executed with unknown exit code: {}", exitCode);
        if ((System.currentTimeMillis() - lastStartTime) >= 5000) {
            LOGGER.info("Restarting process as its execution time was > 5s!");
            Thread.sleep(5000);
            startProcess();
        } else {
            LOGGER.info("Stopping process as it exited too quickly!");
        }
    }

    public boolean handleUpdate(int build) {
        boolean success = Wrapper.getInstance().downloadFiles(build);
        if (success) {
            File versionFile = new File(Wrapper.cascadeWorkingDir, "version.txt");
            try {
                currentVersion = getVersionFromStream(new FileInputStream(versionFile));
            } catch (FileNotFoundException e) {
                getLOGGER().error("Can't find version file", e);
            }
            OperationRunnable.queueOperation(Operation.RESTART);
        }
        return success;
    }

    public synchronized Process getProcess() {
        return process;
    }

    public synchronized AtomicReference<RunState> getState() {
        return state;
    }

    public void stop(boolean force) {
        if (process == null || !process.isAlive()) {
            return;
        }
        state.set(RunState.STOPPING);
        if (force) {
            forceStooped = true;
            process.destroyForcibly();
            state.set(RunState.STOPPED);
        } else {
            PrintWriter writer = new PrintWriter(process.getOutputStream());
            writer.println(Util.getBotCommand("STOP", new String[0]));
            writer.flush();
            writer.close();
        }

        lastStartTime = -1;
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

    public Logger getLOGGER() {
        return LOGGER;
    }

    public Version getVersionFromStream(InputStream stream) {
        StringBuilder builder = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line = reader.readLine();
            builder.append(line);
        } catch (IOException e) {
            getLOGGER().error("Error reading version file", e);
        }
        String versionString = builder.toString();
        getLOGGER().debug(versionString);
        return Version.parseVer(versionString);
    }

    public ProcessStats getProcessStats() {
        return processStats;
    }
}
