package com.test.myproject.signal.network;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SpeedTester {

    private final OkHttpClient client;
    private final Handler mainHandler;
    private boolean isCancelled = false;

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
        String url = "https://speed.cloudflare.com/__down?bytes=25000000";
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (!isCancelled) mainHandler.post(() -> callback.onError("Помилка завантаження: " + e.getMessage()));
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
                byte[] buffer = new byte[8192];

                long totalBytesRead = 0;
                long startTime = System.currentTimeMillis();
                long lastReportTime = startTime;
                long bytesSinceLastReport = 0;

                int bytesRead;
                double smoothedSpeed = 0; // Для плавності даних

                while ((bytesRead = is.read(buffer)) != -1 && !isCancelled) {
                    totalBytesRead += bytesRead;
                    bytesSinceLastReport += bytesRead;
                    long currentTime = System.currentTimeMillis();

                    // Оновлюємо кожні 150 мс для плавної анімації
                    if (currentTime - lastReportTime >= 150) {
                        double deltaSec = (currentTime - lastReportTime) / 1000.0;
                        double currentMbps = ((bytesSinceLastReport * 8.0) / 1_000_000.0) / deltaSec;

                        // Згладжування (Low-pass filter), щоб швидкість не "стрибала" занадто різко
                        smoothedSpeed = smoothedSpeed == 0 ? currentMbps : (smoothedSpeed * 0.4 + currentMbps * 0.6);

                        int progress = (int) ((totalBytesRead * 100) / 25000000);
                        final double reportSpeed = smoothedSpeed;

                        mainHandler.post(() -> callback.onDownloadProgress(reportSpeed, Math.min(progress, 100)));

                        lastReportTime = currentTime;
                        bytesSinceLastReport = 0;
                    }
                }

                is.close();
                response.close();

                final double finalSpeed = smoothedSpeed;
                mainHandler.post(() -> {
                    callback.onDownloadFinished(finalSpeed);
                    onComplete.run();
                });
            }
        });
    }

    private void runUploadTest(SpeedTestCallback callback, Runnable onComplete) {
        String url = "https://speed.cloudflare.com/__up";

        // Генеруємо 10МБ випадкових даних
        byte[] payload = new byte[10000000];
        new Random().nextBytes(payload);

        RequestBody rawBody = RequestBody.create(payload, MediaType.parse("application/octet-stream"));

        final long[] lastReportTime = {System.currentTimeMillis()};
        final long[] bytesSinceLastReport = {0};
        final double[] smoothedSpeed = {0};
        final long startTime = System.currentTimeMillis();

        // Використовуємо наш кастомний ProgressRequestBody
        ProgressRequestBody progressBody = new ProgressRequestBody(rawBody, (bytesWritten, contentLength) -> {
            if (isCancelled) return;

            long currentTime = System.currentTimeMillis();
            long bytesDelta = bytesWritten - bytesSinceLastReport[0];

            if (currentTime - lastReportTime[0] >= 150) {
                double deltaSec = (currentTime - lastReportTime[0]) / 1000.0;
                double currentMbps = ((bytesDelta * 8.0) / 1_000_000.0) / deltaSec;

                smoothedSpeed[0] = smoothedSpeed[0] == 0 ? currentMbps : (smoothedSpeed[0] * 0.4 + currentMbps * 0.6);

                int progress = (int) ((bytesWritten * 100) / contentLength);
                final double reportSpeed = smoothedSpeed[0];

                mainHandler.post(() -> callback.onUploadProgress(reportSpeed, Math.min(progress, 100)));

                lastReportTime[0] = currentTime;
                bytesSinceLastReport[0] = bytesWritten;
            }
        });

        Request request = new Request.Builder().url(url).post(progressBody).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (!isCancelled) mainHandler.post(() -> callback.onError("Помилка вивантаження: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) {
                double timeInSec = (System.currentTimeMillis() - startTime) / 1000.0;
                double finalUploadMbps = (payload.length * 8.0 / 1_000_000.0) / timeInSec;
                response.close();

                mainHandler.post(() -> {
                    callback.onUploadFinished(finalUploadMbps);
                    onComplete.run();
                });
            }
        });
    }
}