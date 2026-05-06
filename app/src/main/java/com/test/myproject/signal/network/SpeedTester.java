package com.test.myproject.signal.network;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SpeedTester {

    private final OkHttpClient client;
    private final Handler mainHandler;

    // Інтерфейс для передачі результатів у MainActivity (на головний потік)
    public interface SpeedTestCallback {
        void onPingResult(long pingMs);
        void onDownloadResult(double mbps);
        void onError(String error);
        void onFinished();
    }

    public SpeedTester() {
        client = new OkHttpClient();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void startDownloadTest(SpeedTestCallback callback) {
        // Використовуємо тестовий сервер Cloudflare. Завантажуємо файл 10 MB.
        // 10 Мегабайт = 80 Мегабіт
        String testUrl = "https://speed.cloudflare.com/__down?bytes=10000000";

        Request request = new Request.Builder()
                .url(testUrl)
                .build();

        final long startTime = System.currentTimeMillis();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError("Помилка сервера: " + response.code()));
                    return;
                }

                // Вимірюємо Ping (час до отримання перших заголовків)
                long pingTime = response.receivedResponseAtMillis() - response.sentRequestAtMillis();
                mainHandler.post(() -> callback.onPingResult(pingTime));

                // Завантажуємо тіло відповіді, щоб виміряти швидкість
                if (response.body() != null) {
                    response.body().bytes(); // Читаємо всі байти
                }

                long endTime = System.currentTimeMillis();
                double timeInSeconds = (endTime - startTime) / 1000.0;

                // Розрахунок: 10 МБ = 80 Мбіт. Швидкість = Мегабіти / час у секундах
                double speedMbps = 80.0 / timeInSeconds;

                mainHandler.post(() -> {
                    callback.onDownloadResult(speedMbps);
                    callback.onFinished();
                });

                response.close();
            }
        });
    }
}