package com.cascadebot.cascadewrapper;

public class CommandHandler {

    private ProcessManager manager;

    public CommandHandler(ProcessManager manager) {
        this.manager = manager;
    }

    public void handleCommand(String[] args) {
        Operation o = Util.getSafeEnum(Operation.class, args[0]);
        if (o == null) {
            return;
        }
        switch (o) {
            case STOP:
                manager.getState().set(RunState.STOPPING);
                break;
            case RESTART:
                manager.getState().set(RunState.STOPPING);
                break;
            case UPDATE:
                int build = Integer.valueOf(args[1]);
                manager.handleUpdate(build);
                break;
            default:
                manager.getLOGGER().info("Received invalid operation from bot: " + o.name());
        }
    }
}
