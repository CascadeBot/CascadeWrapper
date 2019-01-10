package com.cascadebot.cascadewrapper;

import com.cascadebot.cascadewrapper.runnables.OperationRunnable;
import com.cascadebot.cascadewrapper.sockets.WrapperSocketServer;
import com.cascadebot.cascadewrapper.utils.Downloader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class Wrapper {

    public static final Gson GSON = new GsonBuilder().create();
    public static final AtomicBoolean shutdown = new AtomicBoolean(false);
    public static final Logger logger = LoggerFactory.getLogger(Wrapper.class);
    private static Wrapper instance;
    public static String cascadeWorkingDir;

    public static boolean firstInitDone = true;

    private WrapperSocketServer server;

    public static void main(String[] args) {
        (instance = new Wrapper()).init();
    }

    public static Wrapper getInstance() {
        return instance;
    }

    private void init() {
        new Thread(new OperationRunnable()).start();

        server = new WrapperSocketServer(new InetSocketAddress("localhost",8080));
        server.start();

        FileConfiguration config = new YamlConfiguration();
        try {
            config.load("config.ynl");
        } catch (IOException | InvalidConfigurationException e) {
            logger.error("Error loading config", e);
            System.exit(1);
            return;
        }

        cascadeWorkingDir = config.getString("cascade.dir", "../cascade");
        File dir = new File(cascadeWorkingDir);
        if(!dir.exists()) {
            firstInitDone = false;
            firstInit(dir);
        } else if(!dir.isDirectory()) {
            firstInitDone = false;
            firstInit(dir);
        }
    }

    private void firstInit(File dir) {
        if(!dir.mkdirs()) {
            logger.error("Failed to make working directories");
            return;
        }

        String url = "https://jenkins.weeryan17.com/job/Cascade/lastSuccessfulBuild/artifact/target/CascadeBot-jar-with-dependencies.jar";

        try {
            Downloader downloader = new Downloader(new URL(url), new File(dir, "CascadeBot.jar"));
            while (downloader.getStatus() != Downloader.COMPLETE) {

            }
            firstInitDone = true;
        } catch (MalformedURLException e) {
            logger.error("Invalid download url: " + url, e);
        }
    }
}
