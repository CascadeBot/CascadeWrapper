package org.cascadebot.cascadewrapper.utils;

import org.cascadebot.cascadewrapper.Wrapper;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class Downloader implements Runnable {
    private static final int MAX_BUFFER_SIZE = 1024;

    private URL url;
    private File saveLoc;
    private int size;
    private int downloaded;
    private DownloadStatus status;

    public Downloader(URL url, File saveLoc) {
        this.url = url;
        this.saveLoc = saveLoc;
        size = -1;
        downloaded = 0;
        status = DownloadStatus.DOWNLOADING;

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

    public DownloadStatus getStatus() {
        return status;
    }

    public void pause() {
        status = DownloadStatus.PAUSED;
    }

    public void resume() {
        status = DownloadStatus.DOWNLOADING;
        download();
    }

    public void cancel() {
        status = DownloadStatus.CANCELLED;
    }

    private void error() {
        status = DownloadStatus.ERROR;
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
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 Cascade Discord Bot");
            connection.setRequestProperty("Range", "bytes=" + downloaded + "-");

            connection.connect();

            if (connection.getResponseCode() / 100 != 2) {
                Wrapper.logger.error("Got bad response code: " + connection.getResponseCode());
                error();
                return;
            }

            int contentLength = connection.getContentLength();
            if (contentLength < 1) {
                Wrapper.logger.error("Got no content from download");
                error();
                return;
            }

            if (size == -1) {
                size = contentLength;
            }

            file = new RandomAccessFile(this.saveLoc, "rw");
            file.seek(downloaded);

            stream = connection.getInputStream();
            while (status == DownloadStatus.DOWNLOADING) {
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

            if (status == DownloadStatus.DOWNLOADING) {
                status = DownloadStatus.COMPLETE;
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

    public enum DownloadStatus {
        DOWNLOADING,
        PAUSED,
        COMPLETE,
        CANCELLED,
        ERROR;

        @Override
        public String toString() {
            return Character.toTitleCase(name().charAt(0)) + name().substring(1);
        }
    }

}
