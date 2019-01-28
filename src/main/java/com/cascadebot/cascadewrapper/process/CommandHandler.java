package com.cascadebot.cascadewrapper.process;

import com.cascadebot.cascadewrapper.Operation;
import com.cascadebot.cascadewrapper.Util;
import com.cascadebot.cascadewrapper.sockets.WrapperSocketServer;

public class CommandHandler {

    private ProcessManager manager;

    public CommandHandler(ProcessManager manager) {
        this.manager = manager;
    }

    public void handleCommand(String[] args) {
        Operation o = Util.getSafeEnum(Operation.class, args[0]);
        if (o == null) {
            if (args[0].equalsIgnoreCase("authorized")) {
                if(WrapperSocketServer.waitingAuth.containsKey(args[1])) {
                    WrapperSocketServer.authenticatedUsers.add(WrapperSocketServer.waitingAuth.get(args[1]));
                }
                WrapperSocketServer.waitingAuth.remove(args[1]);
            } else if (args[0].equalsIgnoreCase("not_authorized")) {
                WrapperSocketServer.waitingAuth.remove(args[1]);
            }
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
