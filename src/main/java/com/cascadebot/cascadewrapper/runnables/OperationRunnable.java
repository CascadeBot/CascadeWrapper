package com.cascadebot.cascadewrapper.runnables;

import com.cascadebot.cascadewrapper.Operation;
import com.cascadebot.cascadewrapper.Wrapper;
import com.cascadebot.shared.utils.ThreadPoolExecutorLogged;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

public class OperationRunnable implements Runnable {

    private static BlockingQueue<Operation> operationQueue = new LinkedBlockingQueue<>();

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
                            break;
                        case START:
                            break;
                        case STOP:
                            break;
                        case RESTART:
                            break;
                        case UPDATE:
                            break;
                        case FORCE_STOP:
                            break;
                        case FORCE_RESTART:
                            break;
                        case FORCE_UPDATE:
                            break;
                        case WRAPPER_STOP:
                            break;
                    }
                });

            } catch (InterruptedException ignored) {}
        }
    }
}
