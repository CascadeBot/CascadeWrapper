package org.cascadebot.cascadewrapper.runnables;

import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import java.io.IOException;
import org.cascadebot.cascadewrapper.Operation;
import org.cascadebot.cascadewrapper.Wrapper;
import org.cascadebot.cascadewrapper.process.DockerManager;
import org.cascadebot.cascadewrapper.process.ProcessManager;
import org.cascadebot.cascadewrapper.process.RunState;
import org.cascadebot.cascadewrapper.sockets.WrapperSocketServer;
import org.cascadebot.shared.utils.ThreadPoolExecutorLogged;
import org.java_websocket.WebSocket;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class OperationRunnable implements Runnable {

    private static BlockingQueue<Operation> operationQueue = new LinkedBlockingQueue<>();
    protected DockerManager manager;

    protected static OperationRunnable instance;

    public OperationRunnable() {
        instance = this;
        try {
            manager = new DockerManager();
        } catch (DockerCertificateException e) {
            e.printStackTrace();
        }
    }

    public static void queueOperation(Operation operation) {
        operationQueue.add(operation);
    }

    private ThreadGroup operationThreadGroup = new ThreadGroup("Operation thread group"); //TODO threads
    private ExecutorService threadPool = ThreadPoolExecutorLogged.newCachedThreadPool(r -> new Thread(operationThreadGroup, r, operationThreadGroup.getName() + operationThreadGroup.activeCount()), Wrapper.logger);

    @Override
    public void run() {
        Wrapper.logger.info("listing for operations");
        while (!Wrapper.shutdown.get() && !Thread.interrupted()) {
            try {
                Operation operation = operationQueue.take();
                Wrapper.logger.info("Operation: " + operation);
                threadPool.submit(() -> {
                    switch (operation) {
                        case START:
                            if(!manager.getState().get().equals(RunState.STOPPED)) {
                                Wrapper.logger.warn("Start operation tried to be triggered when the process wasn't stopped");
                                return;
                            }
                            try {
                                manager.start();
                            } catch (DockerException | InterruptedException | IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        case STOP:
                            if(!manager.getState().get().equals(RunState.STARTED)) {
                                Wrapper.logger.warn("Stop operation tried to be trigger when the process wasn't running");
                                return;
                            }
                            manager.stop(false);
                            break;
                        case RESTART:
                            manager.stop(false);
                            AtomicBoolean timeOut = new AtomicBoolean(false);
                            AtomicBoolean started = new AtomicBoolean(false);

                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    if (!started.get()) {
                                        timeOut.set(true);
                                        Wrapper.logger.warn("Waiting for process to stop timed out");
                                        WrapperSocketServer socketServer = Wrapper.getInstance().server;
                                        for(WebSocket socket : socketServer.getConnections()) {
                                            socketServer.sendError(socket, "Restart stop timed out!");
                                        }
                                    }
                                }
                            }, TimeUnit.SECONDS.toMillis(30));

                            while (!manager.getState().get().equals(RunState.STOPPED) && !timeOut.get()) {
                                //Do nothing
                            }
                            started.set(true);
                            try {
                                manager.start();
                            } catch (DockerException | InterruptedException | IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        case UPDATE:
                            Wrapper.logger.info("Update called from wrapper.");
                            try {
                                manager.handleUpdate();
                            } catch (DockerException | InterruptedException e) {
                                e.printStackTrace();
                            }
                            break;
                        case FORCE_STOP:
                            manager.stop(true);
                            break;
                        case FORCE_RESTART:
                            manager.stop(true);
                            try {
                                manager.start();
                            } catch (DockerException | InterruptedException | IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        case FORCE_UPDATE:
                            manager.stop(true);
                            try {
                                manager.handleUpdate();
                            } catch (DockerException | InterruptedException e) {
                                e.printStackTrace();
                            }
                            break;
                        case WRAPPER_STOP:
                            System.exit(0);
                            break;
                    }
                });

            } catch (InterruptedException ignored) {}
        }
    }

    public static OperationRunnable getInstance() {
        return instance;
    }

    public DockerManager getManager() {
        return manager;
    }
}
