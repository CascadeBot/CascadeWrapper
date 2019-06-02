package org.cascadebot.cascadewrapper.readers;

import com.spotify.docker.client.LogMessage;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
import org.cascadebot.cascadewrapper.Wrapper;
import org.cascadebot.cascadewrapper.process.DockerManager;

public class DockerConsoleReader implements Runnable {

    DockerManager manager;
    public DockerConsoleReader(DockerManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            LogStream logStream = null;
            try {
                logStream = manager.getDockerClient().attachContainer(manager.getContainerId());
            } catch (DockerException | InterruptedException e) {
                e.printStackTrace();
            }
            while ((logStream.hasNext()) && !Thread.interrupted()) {
                LogMessage logMessage = logStream.next();
                String message = new String(logMessage.content().array());
                Wrapper.logger.info("Bot: " + message);
            }
        }
    }

}
