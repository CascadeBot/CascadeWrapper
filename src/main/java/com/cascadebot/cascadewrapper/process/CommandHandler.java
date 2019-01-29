package com.cascadebot.cascadewrapper.process;

import com.cascadebot.cascadewrapper.Operation;
import com.cascadebot.cascadewrapper.Util;
import com.cascadebot.cascadewrapper.Wrapper;
import com.cascadebot.cascadewrapper.sockets.WrapperSocketServer;

public class CommandHandler {

    private ProcessManager manager;

    public CommandHandler(ProcessManager manager) {
        this.manager = manager;
    }

    public void handleCommand(String[] args) {
        Operation o = Util.getSafeEnum(Operation.class, args[1]);
        if (o == null) {
            if (args[1].equalsIgnoreCase("authorized")) {
                if(WrapperSocketServer.waitingAuth.containsKey(args[2])) {
                    WrapperSocketServer.authenticatedUsers.add(WrapperSocketServer.waitingAuth.get(args[2]).getAttachment());
                    Wrapper.logger.info("Bot authorized user '" + args[2] + "'");
                }
                WrapperSocketServer.waitingAuth.remove(args[2]);
            } else if (args[1].equalsIgnoreCase("not_authorized")) {
                WrapperSocketServer.getInstance().sendError(WrapperSocketServer.waitingAuth.get(args[2]), "User is not authorized to do this!");
                WrapperSocketServer.waitingAuth.remove(args[2]);
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
