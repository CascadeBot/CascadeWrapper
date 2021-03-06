package org.cascadebot.cascadewrapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.cascadebot.cascadewrapper.runnables.OperationRunnable;
import org.cascadebot.cascadewrapper.runnables.ShutdownRunnable;
import org.cascadebot.cascadewrapper.sockets.WrapperSocketServer;
import org.cascadebot.cascadewrapper.utils.Downloader;
import org.cascadebot.shared.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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
    public Auth auth;
    public JedisPool jedis;

    public OkHttpClient httpClient;

    public static boolean firstInitDone = true;

    public WrapperSocketServer server;

    private String url;

    public String botToken;

    public String role;

    private String guild;

    public static void main(String[] args) {
        (instance = new Wrapper()).init();
    }

    public static Wrapper getInstance() {
        return instance;
    }

    private void init() {

        httpClient = new OkHttpClient.Builder().build();

        FileConfiguration config = new YamlConfiguration();
        try {
            config.load("config.yml");
        } catch (IOException | InvalidConfigurationException e) {
            logger.error("Error loading config", e);
            System.exit(1);
            return;
        }

        if(config.contains("redis.password")) {
            jedis = new JedisPool(new JedisPoolConfig(), config.getString("redis.host"), config.getInt("redis.port", 6379), 1000, config.getString("redis.password"), config.getInt("redis.database", 0));
        } else {
            jedis = new JedisPool(new JedisPoolConfig(), config.getString("redis.host"), config.getInt("redis.port", 6379));
        }

        if(config.contains("cascade.sentry")) {
            System.setProperty("SENTRY_DSN", config.getString("cascade.sentry"));
            logger.info("Set sentry system property");
        }

        guild = config.getString("guild");

        botToken = config.getString("bot.token");

        role = config.getString("role");

        url = config.getString("cascade.jenkins");
        cascadeWorkingDir = config.getString("cascade.dir", "../cascade");
        
        int port = config.getInt("wrapper.port", 8080);

        new Thread(new OperationRunnable()).start();

        server = new WrapperSocketServer(new InetSocketAddress("localhost", port));
        server.start();

        try {
            auth = new Auth(config.getString("wrapper.token"));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error creating auth", e);
        }

        File dir = new File(cascadeWorkingDir);
        if (!dir.exists()) {
            firstInitDone = false;
            firstInit(dir);
        } else if (!dir.isDirectory()) {
            firstInitDone = false;
            firstInit(dir);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownRunnable()));
    }

    private void firstInit(File dir) {
        firstInitDone = false; //TODO proper checking
        logger.info("Running initial setup");
        if (!dir.mkdirs()) {
            logger.error("Failed to make working directories");
            return;
        }

        if(downloadFiles(-1)) {

            logger.info("First init done. please go and edit your bots config.");
            System.exit(0);
        } else {
            logger.error("Error downloading files!");
            System.exit(1);
        }
    }

    public boolean downloadFiles(int build) {
        List<String> urls = new ArrayList<>();
        URL versionUrl;
        if(build == -1) {
            urls.add(url + "/lastSuccessfulBuild/artifact/target/CascadeBot-jar-with-dependencies.jar");
            urls.add(url + "/lastSuccessfulBuild/artifact/config.example.yml");
            try {
                versionUrl = new URL(url + "/lastSuccessfulBuild/artifact/target/classes/version.txt");
            } catch (MalformedURLException e) {
                logger.error("Invalid jenkins url", e);
                return false;
            }
        } else {
            urls.add(url + "/" + build + "/artifact/target/CascadeBot-jar-with-dependencies.jar");
            urls.add(url + "/" + build + "/artifact/config.example.yml");
            try {
                versionUrl = new URL(url + "/" + build + "/artifact/target/classes/version.txt");
            } catch (MalformedURLException e) {
                logger.error("Invalid jenkins url", e);
                return false;
            }

        }

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
                while (downloader.getStatus() == Downloader.DownloadStatus.DOWNLOADING) {
                    logger.info("Downloading file: '" + getName(downloader.getUrl()) + "' " + downloader.getProgress() + "%");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
                if (downloader.getStatus() != Downloader.DownloadStatus.COMPLETE) {
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

        try {
            logger.info("Downloading version file");
            Files.copy(versionUrl.openStream(), new File(cascadeWorkingDir, "version.txt").toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Error downloading version file.", e);
            return false;
        }

        return !error.get();

    }

    private String getName(String url) {
        String[] split = url.split("/");
        return split[split.length - 1];
    }

    public String getUrl() {
        return url;
    }

    public String getGuild() {
        return guild;
    }
}
