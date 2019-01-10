package com.cascadebot.cascadewrapper.runnables;

import com.cascadebot.cascadewrapper.Operation;
import com.cascadebot.cascadewrapper.Wrapper;
import com.cascadebot.shared.utils.ThreadPoolExecutorLogged;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

public class OperationRunnable implements Runnable {

    private static BlockingQueue<Operation> operationQueue = new LinkedBlockingQueue<>();
    private ProcessManager manager;

    public static void queueOperation(Operation operation) {
        operationQueue.add(operation);
    }

    private ThreadGroup operationThreadGroup = new ThreadGroup("Operation thread group"); //TODO threads
    private ExecutorService threadPool = ThreadPoolExecutorLogged.newCachedThreadPool(r -> new Thread(operationThreadGroup, r, operationThreadGroup.getName() + operationThreadGroup.activeCount()), Wrapper.logger);

    @Override
    public void run() {
        while (!Wrapper.shutdown.get() && !Thread.interrupted()) {
            try {
                Operation operation = operationQueue.take();
                threadPool.submit(() -> {
                    switch (operation) {

                        case NOOP:
                            //TODO figure out what this is suppose to do (looking at you binary)
                            break;
                        case START:
                            manager = new ProcessManager("CascadeBot.jar", new String[]{});
                            break;
                        case STOP:
                            manager.getProcess().destroy();
                            break;
                        case RESTART:
                            manager.getProcess().destroy();
                            manager = new ProcessManager("CascadeBot.jar", new String[]{});
                            break;
                        case UPDATE:
                            manager.handleUpdate();
                            break;
                        case FORCE_STOP:
                            manager.getProcess().destroyForcibly();
                            break;
                        case FORCE_RESTART:
                            manager.getProcess().destroyForcibly();
                            manager = new ProcessManager("CascadeBot.jar", new String[]{});
                            break;
                        case FORCE_UPDATE:
                            manager.getProcess().destroyForcibly();
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
