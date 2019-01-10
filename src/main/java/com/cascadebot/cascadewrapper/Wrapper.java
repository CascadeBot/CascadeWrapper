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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class Wrapper {

    public static final Gson GSON = new GsonBuilder().create();
    public static final AtomicBoolean shutdown = new AtomicBoolean(false);
    public static final Logger logger = LoggerFactory.getLogger(Wrapper.class);
    private static Wrapper instance;
    public static String cascadeWorkingDir;

    public static boolean firstInitDone = true;

    private WrapperSocketServer server;

    private List<String> urls;

    public static void main(String[] args) {
        (instance = new Wrapper()).init();
    }

    public static Wrapper getInstance() {
        return instance;
    }

    private void init() {
        new Thread(new OperationRunnable()).start();

        server = new WrapperSocketServer(new InetSocketAddress("localhost", 8080));
        server.start();

        FileConfiguration config = new YamlConfiguration();
        try {
            config.load("config.yml");
        } catch (IOException | InvalidConfigurationException e) {
            logger.error("Error loading config", e);
            System.exit(1);
            return;
        }

        cascadeWorkingDir = config.getString("cascade.dir", "../cascade");

        urls = config.getStringList("cascade.downloads");

        File dir = new File(cascadeWorkingDir);
        if (!dir.exists()) {
            firstInitDone = false;
            firstInit(dir);
        } else if (!dir.isDirectory()) {
            firstInitDone = false;
            firstInit(dir);
        }
    }

    private void firstInit(File dir) {
        firstInitDone = false; //TODO proper checking
        logger.info("Running initial setup");
        if (!dir.mkdirs()) {
            logger.error("Failed to make working directories");
            return;
        }

        if(downloadFiles()) {

            logger.info("First init done. please go and edit your bots config.");
            System.exit(0);
        } else {
            logger.error("Error downloading files!");
            System.exit(1);
        }
    }

    public boolean downloadFiles() {
        boolean downloadDone = false;
        AtomicBoolean error = new AtomicBoolean(false);
        List<Downloader> downloaders = new ArrayList<>();
        for (String download : urls) {
            try {
                Downloader downloader = new Downloader(new URL(download), new File(cascadeWorkingDir, getName(download)));
                downloaders.add(downloader);
            } catch (MalformedURLException e) {
                logger.error("Invalid url: " + download, e);
                error.set(true);
            }
        }

        if (error.get()) {
            return false;
        }

        Map<String, Boolean> doneMap = new HashMap<>();

        for (Downloader downloader : downloaders) {
            doneMap.put(getName(downloader.getUrl()), false);
            new Thread(() -> {
                while (downloader.getStatus() == Downloader.DOWNLOADING) {
                    logger.info("Downloading file: '" + getName(downloader.getUrl()) + "' " + downloader.getProgress() + "%");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
                if (downloader.getStatus() != Downloader.COMPLETE) {
                    logger.error("Couldn't download file: " + getName(downloader.getUrl()) + "\nStatus: " + downloader.getStatus());
                    error.set(true);
                } else {
                    logger.info("Done downloading " + getName(downloader.getUrl()));
                    doneMap.put(getName(downloader.getUrl()), true);
                }
            }).start();
        }

        while (!downloadDone) {
            boolean done = true;
            for(Map.Entry<String, Boolean> entry : doneMap.entrySet()) {
                if(!entry.getValue()) {
                    done = false;
                }
            }
            downloadDone = done;
            firstInitDone = done;
        }

        return !error.get();

    }

    private String getName(String url) {
        String[] split = url.split("/");
        return split[split.length - 1];
    }
}
