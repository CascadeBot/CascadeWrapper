package com.cascadebot.cascadewrapper.process;

import com.cascadebot.cascadewrapper.Operation;
import com.cascadebot.cascadewrapper.JSONObject;
import com.cascadebot.cascadewrapper.Util;
import com.cascadebot.cascadewrapper.Wrapper;
import com.cascadebot.cascadewrapper.sockets.Packet;
import com.cascadebot.cascadewrapper.sockets.SessionInfo;
import com.cascadebot.cascadewrapper.sockets.WrapperSocketServer;
import com.cascadebot.shared.OpCodes;
import org.java_websocket.WebSocket;

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
                    WebSocket conn = WrapperSocketServer.waitingAuth.get(args[2]);

                    SessionInfo info = conn.getAttachment();
                    info.setSecurityLevel(args[3]);
                    conn.setAttachment(info);

                    JSONObject operationJson = new JSONObject();
                    operationJson.add("authorized", true);
                    operationJson.add("sessionid", ((SessionInfo)conn.getAttachment()).getUuid().toString());
                    conn.send(new Packet(OpCodes.AUTHORISE, operationJson).toJSON());

                    WrapperSocketServer.authenticatedUsers.add(info);
                    Wrapper.logger.info("Bot authorized user '" + args[2] + "' with level " + args[3]);
                }
                WrapperSocketServer.waitingAuth.remove(args[2]);
            } else if (args[1].equalsIgnoreCase("not_authorized")) {
                if(!WrapperSocketServer.waitingAuth.containsKey(args[2])) {
                    return;
                }
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
