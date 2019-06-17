package org.cascadebot.cascadewrapper.process;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import org.cascadebot.cascadewrapper.Operation;
import org.cascadebot.cascadewrapper.Util;
import org.cascadebot.cascadewrapper.Wrapper;
import org.cascadebot.cascadewrapper.sockets.SessionInfo;
import org.cascadebot.cascadewrapper.sockets.WrapperSocketServer;
import org.java_websocket.WebSocket;

public class CommandHandler {

    private DockerManager manager;

    public CommandHandler(DockerManager manager) {
        this.manager = manager;
        manager.commandHandler = this;
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

                    WrapperSocketServer.getInstance().sendAuthorisedPacket(conn);

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
            case RESTART:
                manager.getState().set(RunState.STOPPING);
                break;
            case UPDATE:
                int build = Integer.valueOf(args[1]);
                try {
                    manager.handleUpdate();
                } catch (DockerException | InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            default:
                DockerManager.getLOGGER().info("Received invalid operation from bot: " + o.name());
        }
    }
}
