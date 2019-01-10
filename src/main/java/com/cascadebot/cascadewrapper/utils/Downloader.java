package com.cascadebot.cascadewrapper.utils;

import com.cascadebot.cascadewrapper.Wrapper;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class Downloader implements Runnable {
    private static final int MAX_BUFFER_SIZE = 1024;

    public static final String[] STATUSES = {"Downloading", "Paused", "Complete", "Cancelled", "Error"};

    public static final int DOWNLOADING = 0;
    public static final int PAUSED = 1;
    public static final int COMPLETE = 2;
    public static final int CANCELLED = 3;
    public static final int ERROR = 4;

    private URL url;
    private File saveLoc;
    private int size;
    private int downloaded;
    private int status;

    public Downloader(URL url, File saveLoc) {
        this.url = url;
        this.saveLoc = saveLoc;
        size = -1;
        downloaded = 0;
        status = DOWNLOADING;

        download();
    }

    public String getUrl() {
        return url.toString();
    }

    public int getSize() {
        return size;
    }

    public float getProgress() {
        return ((float) downloaded / size) * 100;
    }

    public int getStatus() {
        return status;
    }

    public void pause() {
        status = PAUSED;
    }

    public void resume() {
        status = DOWNLOADING;
        download();
    }

    public void cancel() {
        status = CANCELLED;
    }

    private void error() {
        status = ERROR;
    }

    private void download() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run() {
        RandomAccessFile file = null;
        InputStream stream = null;

        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Range", "bytes=" + downloaded + "-");

            connection.connect();

            if (connection.getResponseCode() / 100 != 2) {
                Wrapper.logger.error("Got bad response code: " + connection.getResponseCode());
                error();
            }

            int contentLength = connection.getContentLength();
            if (contentLength < 1) {
                Wrapper.logger.error("Got no content from download");
                error();
            }

            if (size == -1) {
                size = contentLength;
            }

            file = new RandomAccessFile(this.saveLoc, "rw");
            file.seek(downloaded);

            stream = connection.getInputStream();
            while (status == DOWNLOADING) {
                byte buffer[];
                if (size - downloaded > MAX_BUFFER_SIZE) {
                    buffer = new byte[MAX_BUFFER_SIZE];
                } else {
                    buffer = new byte[size - downloaded];
                }

                int read = stream.read(buffer);
                if (read == -1)
                    break;

                file.write(buffer, 0, read);
                downloaded += read;
            }

            if (status == DOWNLOADING) {
                status = COMPLETE;
            }
        } catch (Exception e) {
            Wrapper.logger.error("Error while download", e);
            error();
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (Exception e) {
                }
            }

            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
