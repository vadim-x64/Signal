package com.test.myproject.signal.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TestResult {
    private final double downloadSpeed;
    private final double uploadSpeed;
    private final long ping;
    private final long timestamp;

    public TestResult(double downloadSpeed, double uploadSpeed, long ping) {
        this.downloadSpeed = downloadSpeed;
        this.uploadSpeed = uploadSpeed;
        this.ping = ping;
        this.timestamp = System.currentTimeMillis();
    }

    public double getDownloadSpeed() {
        return downloadSpeed;
    }

    public double getUploadSpeed() {
        return uploadSpeed;
    }

    public long getPing() {
        return ping;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", new Locale("uk", "UA"));
        return sdf.format(new Date(timestamp));
    }
}