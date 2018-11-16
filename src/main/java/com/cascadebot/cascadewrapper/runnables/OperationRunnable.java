package com.cascadebot.cascadewrapper.runnables;

import com.cascadebot.cascadewrapper.Operation;
import com.cascadebot.cascadewrapper.Wrapper;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class OperationRunnable implements Runnable {

    private BlockingQueue<Operation> operationQueue = new LinkedBlockingDeque<>();


    @Override
    public void run() {
        while (!Wrapper.shutdown.get() && !Thread.interrupted()) {
            try {
                Operation operation = operationQueue.take();

            } catch (InterruptedException ignored) {}
        }
    }
}
