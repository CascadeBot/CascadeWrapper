package com.cascadebot.cascadewrapper.process;

import com.cascadebot.cascadewrapper.Wrapper;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class ProcessStats extends TimerTask {

    private ProcessManager manager;

    private long lastRun = 0;
    private long lastCpu = 0;

    public double cpu = 0;

    public ProcessStats(ProcessManager manager) {
        this.manager = manager;
        new Timer().schedule(this, 0, TimeUnit.SECONDS.toMillis(5));
    }

    @Override
    public void run() {
        if (manager.getProcess() == null || manager.getState().get() == RunState.STOPPED) {
            return;
        }
        if (!manager.getProcess().info().totalCpuDuration().isPresent()) {
            return;
        }

        long run = System.currentTimeMillis() - manager.getLastStartTime();
        long cpu = TimeUnit.NANOSECONDS.toMillis(manager.getProcess().info().totalCpuDuration().get().getNano());

        long currentRun = run - lastRun;
        long currentCpu = cpu - lastCpu;

        lastRun = run;
        lastCpu = cpu;

        BigDecimal runDec = new BigDecimal(currentRun);
        BigDecimal cpuDec = new BigDecimal(currentCpu);

        BigDecimal percent = cpuDec.divide(runDec, MathContext.DECIMAL32);
        this.cpu = percent.doubleValue();
    }
}
