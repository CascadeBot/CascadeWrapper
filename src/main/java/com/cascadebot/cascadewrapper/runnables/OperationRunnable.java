package com.cascadebot.cascadewrapper.runnables;

import com.cascadebot.cascadewrapper.Operation;
import com.cascadebot.cascadewrapper.ProcessManager;
import com.cascadebot.cascadewrapper.RunState;
import com.cascadebot.cascadewrapper.Wrapper;
import com.cascadebot.cascadewrapper.sockets.WrapperSocketServer;
import com.cascadebot.shared.utils.ThreadPoolExecutorLogged;
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
    protected ProcessManager manager;

    protected static OperationRunnable instance;

    public OperationRunnable() {
        instance = this;
        manager = new ProcessManager("CascadeBot-jar-with-dependencies.jar", new String[]{}); //TODO mayBe add config options for file names
    }

    public static void queueOperation(Operation operation) {
        operationQueue.add(operation);
    }

    private ThreadGroup operationThreadGroup = new ThreadGroup("Operation thread group"); //TODO threads
    private ExecutorService threadPool = ThreadPoolExecutorLogged.newCachedThreadPool(r -> new Thread(operationThreadGroup, r, operationThreadGroup.getName() + operationThreadGroup.activeCount()), Wrapper.logger);

    @Override
    public void run() {
        manager.handleUpdate("1.0.2");
        Wrapper.logger.info("listing for operations");
        while (!Wrapper.shutdown.get() && !Thread.interrupted()) {
            try {
                Operation operation = operationQueue.take();
                Wrapper.logger.info("Operation: " + operation);
                threadPool.submit(() -> {
                    switch (operation) {

                        case NOOP:
                            //TODO figure out what this is suppose to do (looking at you binary)
                            break;
                        case START:
                            if(!manager.getState().get().equals(RunState.STOPPED)) {
                                Wrapper.logger.warn("Start operation tried to be triggered when the process wasn't stopped");
                                return;
                            }
                            manager.start();
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
                            manager.start();
                            break;
                        case UPDATE:
                            manager.handleUpdate();
                            break;
                        case FORCE_STOP:
                            manager.stop(true);
                            break;
                        case FORCE_RESTART:
                            manager.stop(true);
                            manager.start();
                            break;
                        case FORCE_UPDATE:
                            manager.stop(true);
                            manager.handleUpdate();
                            break;
                        case WRAPPER_STOP:
                            System.exit(0);
                            break;
                    }
                });

            } catch (InterruptedException ignored) {}
        }
    }
}
