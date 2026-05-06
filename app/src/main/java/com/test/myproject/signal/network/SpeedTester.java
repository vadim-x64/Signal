package com.test.myproject.signal.network;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
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
        // Налаштовуємо таймаути для точніших тестів
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
        // Завантажуємо файл ~25MB для тесту
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
                byte[] buffer = new byte[8192]; // Читаємо по 8КБ
                long totalBytesRead = 0;
                long startTime = System.currentTimeMillis();
                long lastReportTime = startTime;
                int bytesRead;

                double latestMbps = 0;

                while ((bytesRead = is.read(buffer)) != -1 && !isCancelled) {
                    totalBytesRead += bytesRead;
                    long currentTime = System.currentTimeMillis();

                    // Оновлюємо UI кожні 200 мс
                    if (currentTime - lastReportTime > 200) {
                        double timeInSec = (currentTime - startTime) / 1000.0;
                        latestMbps = (totalBytesRead * 8.0 / 1_000_000.0) / timeInSec;
                        // Симуляція відсотка (25MB = 25 000 000 байт)
                        int progress = (int) ((totalBytesRead * 100) / 25000000);

                        final double reportSpeed = latestMbps;
                        mainHandler.post(() -> callback.onDownloadProgress(reportSpeed, Math.min(progress, 100)));
                        lastReportTime = currentTime;
                    }
                }

                is.close();
                response.close();

                final double finalSpeed = latestMbps;
                mainHandler.post(() -> {
                    callback.onDownloadFinished(finalSpeed);
                    onComplete.run();
                });
            }
        });
    }

    private void runUploadTest(SpeedTestCallback callback, Runnable onComplete) {
        String url = "https://speed.cloudflare.com/__up";
        // Генеруємо 10МБ випадкових даних для відправки
        byte[] payload = new byte[10000000];
        RequestBody body = RequestBody.create(payload, MediaType.parse("application/octet-stream"));

        Request request = new Request.Builder().url(url).post(body).build();
        long startTime = System.currentTimeMillis();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (!isCancelled) mainHandler.post(() -> callback.onError("Помилка вивантаження: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) {
                long endTime = System.currentTimeMillis();
                double timeInSec = (endTime - startTime) / 1000.0;
                // 10МБ = 80 Мегабіт
                double uploadMbps = (payload.length * 8.0 / 1_000_000.0) / timeInSec;

                response.close();

                mainHandler.post(() -> {
                    // Оскільки OkHttp не дає легкого доступу до потоку відправки,
                    // ми просто симулюємо прогрес 100% після завершення
                    callback.onUploadProgress(uploadMbps, 100);
                    callback.onUploadFinished(uploadMbps);
                    onComplete.run();
                });
            }
        });
    }
}