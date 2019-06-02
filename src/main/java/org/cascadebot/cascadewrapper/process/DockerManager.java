package org.cascadebot.cascadewrapper.process;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ImageNotFoundException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.cascadebot.cascadewrapper.Util;
import org.cascadebot.cascadewrapper.readers.DockerConsoleReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessManager.class);

    private DockerClient dockerClient;
    private ContainerCreation container;

    private AtomicReference<RunState> state = new AtomicReference<>();

    public DockerManager() throws DockerCertificateException {
        state.set(RunState.STOPPED);
        dockerClient = DefaultDockerClient.fromEnv().build();
    }

    public void handleUpdate() throws DockerException, InterruptedException {
        LOGGER.info("Downloading latest image");
        dockerClient.pull("cascadebot/cascadebot"); //TODO handle versions, and restarting
    }

    public void start() throws DockerException, InterruptedException {
        try {
            dockerClient.inspectImage("cascadebot/cascadebot:1.0"); //TODO not hardcode this
        } catch (ImageNotFoundException e) {
            handleUpdate();
        }

        String[] ports = {"8080"}; //TODO attach to running container

        Map<String, List<PortBinding>> portBindings = new HashMap<>();
        for (String port : ports) {
            List<PortBinding> hostPorts = new ArrayList<>();
            hostPorts.add(PortBinding.of("0.0.0.0", port));
            portBindings.put(port, hostPorts);
        }

        HostConfig hostConfig = HostConfig.builder().portBindings(portBindings).build();

        ContainerConfig containerConfig = ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image("cascadebot/cascadebot:1.0")
                .exposedPorts(ports)
                .build();

        container = dockerClient.createContainer(containerConfig);
        state.set(RunState.STARTING);
        dockerClient.startContainer(container.id());
        state.set(RunState.STARTED);

        LogStream logStream = dockerClient.attachContainer(container.id());
        new Thread(new DockerConsoleReader(this)).start();
    }

    public AtomicReference<RunState> getState() {
        return state;
    }

    public static Logger getLOGGER() {
        return LOGGER;
    }

    public void stop(boolean force) {
        state.set(RunState.STOPPING);
        try {
            if (force) {
                dockerClient.killContainer(container.id());
                state.set(RunState.STOPPED);
                dockerClient.removeContainer(container.id());
            } else {
                dockerClient.execCreate(container.id(), new String[]{Util.getBotCommand("STOP", new String[0])});
                while (dockerClient.inspectContainer(container.id()).state().running()) {
                    //Wait for bot to stop
                }
                state.set(RunState.STOPPED);
                dockerClient.removeContainer(container.id());
                //TODO detect when container is stopped
            }
        } catch (DockerException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public DockerClient getDockerClient() {
        return dockerClient;
    }

    public String getContainerId() {
        return container == null ? null : container.id();
    }
}
