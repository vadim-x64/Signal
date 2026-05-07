package com.test.myproject.signal.network;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

public class SpeedTester {
    private final OkHttpClient client;
    private final Handler mainHandler;
    private boolean isCancelled = false;

    private static class SpeedDataPoint {
        long timestamp;
        long bytes;

        SpeedDataPoint(long t, long b) {
            timestamp = t;
            bytes = b;
        }
    }

    public interface SpeedTestCallback {
        void onPingResult(long pingMs);

        void onDownloadProgress(double mbps, int progressPercent);

        void onDownloadFinished(double finalMbps);

        void onUploadProgress(double mbps, int progressPercent);

        void onUploadFinished(double finalMbps);

        void onError(String error);

        void onFinished(long totalDurationMs);
    }

    public SpeedTester() {
        client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void cancelTest() {
        isCancelled = true;
        client.dispatcher().cancelAll();
    }

    public void startFullTest(SpeedTestCallback callback) {
        isCancelled = false;
        long globalStartTime = System.currentTimeMillis();

        runDownloadTest(callback, () -> {
            if (isCancelled) return;
            runUploadTest(callback, () -> {
                long totalTime = System.currentTimeMillis() - globalStartTime;
                mainHandler.post(() -> callback.onFinished(totalTime));
            });
        });
    }

    private void runDownloadTest(SpeedTestCallback callback, Runnable onComplete) {
        String url = "https://speed.cloudflare.com/__down?bytes=50000000";
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (!isCancelled)
                    mainHandler.post(() -> callback.onError("Помилка завантаження: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError("Помилка сервера (DL): " + response.code()));
                    return;
                }

                long pingMs = response.receivedResponseAtMillis() - response.sentRequestAtMillis();
                mainHandler.post(() -> callback.onPingResult(pingMs));

                InputStream is = response.body().byteStream();

                byte[] buffer = new byte[16384];
                long totalBytesRead = 0;
                long startTime = System.currentTimeMillis();
                long lastReportTime = startTime;

                LinkedList<SpeedDataPoint> rollingWindow = new LinkedList<>();

                double displaySpeed = 0;
                int bytesRead;

                while ((bytesRead = is.read(buffer)) != -1 && !isCancelled) {
                    totalBytesRead += bytesRead;
                    long currentTime = System.currentTimeMillis();
                    rollingWindow.add(new SpeedDataPoint(currentTime, totalBytesRead));

                    while (!rollingWindow.isEmpty() && currentTime - rollingWindow.getFirst().timestamp > 1000) {
                        rollingWindow.removeFirst();
                    }

                    if (currentTime - lastReportTime >= 200) {
                        if (rollingWindow.size() > 1) {
                            SpeedDataPoint oldest = rollingWindow.getFirst();
                            SpeedDataPoint newest = rollingWindow.getLast();

                            double deltaSec = (newest.timestamp - oldest.timestamp) / 1000.0;
                            long deltaBytes = newest.bytes - oldest.bytes;

                            if (deltaSec > 0) {
                                double instantSpeed = ((deltaBytes * 8.0) / 1_000_000.0) / deltaSec;
                                displaySpeed = displaySpeed == 0 ? instantSpeed : (displaySpeed * 0.7 + instantSpeed * 0.3);
                            }
                        }

                        int progress = (int) ((totalBytesRead * 100) / 50000000);
                        final double reportSpeed = displaySpeed;
                        mainHandler.post(() -> callback.onDownloadProgress(reportSpeed, Math.min(progress, 100)));
                        lastReportTime = currentTime;
                    }
                }
                is.close();
                response.close();

                double totalTimeSec = (System.currentTimeMillis() - startTime) / 1000.0;
                double exactFinalSpeed = ((totalBytesRead * 8.0) / 1_000_000.0) / totalTimeSec;

                mainHandler.post(() -> {
                    callback.onDownloadFinished(exactFinalSpeed);
                    mainHandler.postDelayed(onComplete, 1000);
                });
            }
        });
    }

    private void runUploadTest(SpeedTestCallback callback, Runnable onComplete) {
        String url = "https://speed.cloudflare.com/__up";

        final long[] finalBytesWritten = {0};
        final long[] finalTimeMs = {0};

        RequestBody requestBody = new RequestBody() {
            @Override
            public MediaType contentType() {
                return MediaType.parse("application/octet-stream");
            }

            @Override
            public long contentLength() {
                return -1;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                byte[] chunk = new byte[128 * 1024];
                new Random().nextBytes(chunk);

                long bytesWritten = 0;
                long startTime = System.currentTimeMillis();
                long testDurationMs = 8000;
                long lastReportTime = startTime;
                double displaySpeed = 0;

                while (System.currentTimeMillis() - startTime < testDurationMs && !isCancelled) {
                    sink.write(chunk);
                    sink.flush();
                    bytesWritten += chunk.length;

                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastReportTime >= 250) {
                        double totalTimeSec = (currentTime - startTime) / 1000.0;

                        if (totalTimeSec > 0.3) {
                            double currentSpeed = ((bytesWritten * 8.0) / 1_000_000.0) / totalTimeSec;
                            displaySpeed = displaySpeed == 0 ? currentSpeed : (displaySpeed * 0.8 + currentSpeed * 0.2);
                        }

                        int progress = (int) (((currentTime - startTime) * 100) / testDurationMs);
                        final double reportSpeed = displaySpeed;
                        mainHandler.post(() -> callback.onUploadProgress(reportSpeed, Math.min(progress, 100)));
                        lastReportTime = currentTime;
                    }
                }

                finalBytesWritten[0] = bytesWritten;
                finalTimeMs[0] = System.currentTimeMillis() - startTime;
            }
        };

        final long testStartTime = System.currentTimeMillis();
        Request request = new Request.Builder().url(url).post(requestBody).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (!isCancelled && !e.getMessage().contains("Canceled") && !e.getMessage().contains("Socket closed")) {
                    mainHandler.post(() -> callback.onError("Помилка вивантаження: " + e.getMessage()));
                } else if (!isCancelled) {
                    reportFinalUpload(callback, onComplete, finalBytesWritten[0], finalTimeMs[0]);
                }
            }

            @Override
            public void onResponse(Call call, Response response) {
                response.close();
                reportFinalUpload(callback, onComplete, finalBytesWritten[0], finalTimeMs[0]);
            }
        });
    }

    private void reportFinalUpload(SpeedTestCallback callback, Runnable onComplete, long bytes, long timeMs) {
        if (timeMs == 0) timeMs = 1;
        double exactFinalSpeed = ((bytes * 8.0) / 1_000_000.0) / (timeMs / 1000.0);
        mainHandler.post(() -> {
            callback.onUploadFinished(exactFinalSpeed);
            onComplete.run();
        });
    }
}