package org.cascadebot.cascadewrapper.readers;

import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import org.cascadebot.cascadewrapper.process.DockerManager;

public class DockerConsoleReader implements Runnable {

    private DockerManager manager;
    public DockerConsoleReader(DockerManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            LogStream logStream = null;
            try {
                logStream = manager.getDockerClient().attachContainer(manager.getContainerId());
                PipedInputStream stdout = new PipedInputStream();
                PipedInputStream stderr = new PipedInputStream();

                PipedOutputStream outStdout = new PipedOutputStream(stdout);
                PipedOutputStream outStderr = new PipedOutputStream(stderr);
                logStream.attach(outStdout, outStderr);

                new Thread(new ConsoleReader(stdout, manager.commandHandler)).start();
                new Thread(new ErrorReader(stderr)).start();
            } catch (DockerException | InterruptedException | IOException e) {
                e.printStackTrace();
            }

            /*while ((logStream.hasNext()) && !Thread.interrupted()) {
                LogMessage logMessage = logStream.next();
                String message = new String(logMessage.content().array());
                Wrapper.logger.info("Bot: " + message);
            }*/
        }
    }

}
