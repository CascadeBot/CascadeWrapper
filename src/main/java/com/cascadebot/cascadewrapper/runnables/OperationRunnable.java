package com.cascadebot.cascadewrapper.runnables;

import com.cascadebot.cascadewrapper.Operation;
import com.cascadebot.cascadewrapper.Wrapper;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class OperationRunnable implements Runnable {

    private BlockingQueue<Operation> operationQueue = new LinkedBlockingQueue<>();


    @Override
    public void run() {
        while (!Wrapper.shutdown.get() && !Thread.interrupted()) {
            try {
                Operation operation = operationQueue.take();

            } catch (InterruptedException ignored) {}
        }
    }
}
