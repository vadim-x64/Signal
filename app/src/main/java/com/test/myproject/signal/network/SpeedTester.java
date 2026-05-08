package com.test.myproject.signal.network;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

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
import okio.BufferedSink;

public class SpeedTester {
    private final OkHttpClient client;
    private final Handler mainHandler;
    private boolean isCancelled = false;

    public interface SpeedTestCallback {
        void onServerInfo(String location, String ip);
        void onHandshakeStart();
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
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .addInterceptor(chain -> {
                    okhttp3.Request original = chain.request();
                    okhttp3.Request request = original.newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile; rv:125.0) Gecko/125.0 Firefox/125.0")
                            .header("Accept", "*/*")
                            .build();
                    return chain.proceed(request);
                })
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

        mainHandler.post(callback::onHandshakeStart);

        fetchServerMeta(callback, () -> {
            if (isCancelled) return;
            runPingTest(callback, () -> {
                if (isCancelled) return;
                runDownloadTest(callback, () -> {
                    if (isCancelled) return;
                    runUploadTest(callback, () -> {
                        long totalTime = System.currentTimeMillis() - globalStartTime;
                        mainHandler.post(() -> callback.onFinished(totalTime));
                    });
                });
            });
        });
    }

    private void fetchServerMeta(SpeedTestCallback callback, Runnable onComplete) {
        Request request = new Request.Builder().url("https://speed.cloudflare.com/meta").build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onServerInfo("Невідомий сервер", "---"));
                onComplete.run();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonStr = response.body().string();
                        JSONObject json = new JSONObject(jsonStr);
                        String city = json.optString("city", "Невідомо");
                        String country = json.optString("country", "");
                        String ip = json.optString("clientIp", "");
                        String colo = json.optString("colo", "");

                        String location = city + (country.isEmpty() ? "" : ", " + country) + " (" + colo + ")";
                        mainHandler.post(() -> callback.onServerInfo(location, ip));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onServerInfo("Невідомий сервер", "---"));
                    }
                }
                response.close();
                onComplete.run();
            }
        });
    }

    private void runPingTest(SpeedTestCallback callback, Runnable onComplete) {
        Request request = new Request.Builder().url("https://speed.cloudflare.com/cdn-cgi/trace").build();
        long startPingTime = System.currentTimeMillis();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (!isCancelled) mainHandler.post(() -> callback.onError("Помилка Ping: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) {
                long pingMs = System.currentTimeMillis() - startPingTime;
                response.close();
                mainHandler.post(() -> {
                    callback.onPingResult(pingMs);
                    onComplete.run();
                });
            }
        });
    }

    private void runDownloadTest(SpeedTestCallback callback, Runnable onComplete) {
        String url = "https://speed.cloudflare.com/__down?bytes=50000000";
        Request request = new Request.Builder()
                .url(url)
                .header("Cache-Control", "no-cache")
                .build();

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

                InputStream is = response.body().byteStream();
                byte[] buffer = new byte[128 * 1024];
                long totalBytesRead = 0;
                long targetBytes = 50000000;

                long startTimeNano = System.nanoTime();
                long lastReportTimeNano = startTimeNano;
                double displaySpeed = 0;

                int bytesRead;

                while ((bytesRead = is.read(buffer)) != -1 && !isCancelled) {
                    totalBytesRead += bytesRead;
                    long currentTimeNano = System.nanoTime();

                    if (currentTimeNano - lastReportTimeNano >= 150_000_000L) {
                        double deltaSec = (currentTimeNano - startTimeNano) / 1_000_000_000.0;
                        if (deltaSec > 0.1) {
                            double instantSpeed = ((totalBytesRead * 8.0) / 1_000_000.0) / deltaSec;
                            displaySpeed = displaySpeed == 0 ? instantSpeed : (displaySpeed * 0.8 + instantSpeed * 0.2);
                        }

                        int progress = (int) ((totalBytesRead * 100) / targetBytes);
                        final double reportSpeed = displaySpeed;
                        mainHandler.post(() -> callback.onDownloadProgress(reportSpeed, Math.min(progress, 100)));
                        lastReportTimeNano = currentTimeNano;
                    }
                }
                is.close();
                response.close();

                double totalTimeSec = (System.nanoTime() - startTimeNano) / 1_000_000_000.0;
                double exactFinalSpeed = ((totalBytesRead * 8.0) / 1_000_000.0) / totalTimeSec;

                mainHandler.post(() -> {
                    callback.onDownloadFinished(exactFinalSpeed);
                    mainHandler.postDelayed(onComplete, 500);
                });
            }
        });
    }

    private void runUploadTest(SpeedTestCallback callback, Runnable onComplete) {
        String url = "https://speed.cloudflare.com/__up";

        final long[] finalBytesWritten = {0};
        final long[] finalTimeNano = {0};

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
                long startTimeNano = System.nanoTime();
                long testDurationNano = 8_000_000_000L;
                long lastReportTimeNano = startTimeNano;
                double displaySpeed = 0;

                while ((System.nanoTime() - startTimeNano) < testDurationNano && !isCancelled) {
                    sink.write(chunk);
                    sink.flush();
                    bytesWritten += chunk.length;

                    long currentTimeNano = System.nanoTime();
                    if (currentTimeNano - lastReportTimeNano >= 150_000_000L) {
                        double totalTimeSec = (currentTimeNano - startTimeNano) / 1_000_000_000.0;

                        if (totalTimeSec > 0.2) {
                            double currentSpeed = ((bytesWritten * 8.0) / 1_000_000.0) / totalTimeSec;
                            displaySpeed = displaySpeed == 0 ? currentSpeed : (displaySpeed * 0.8 + currentSpeed * 0.2);
                        }

                        int progress = (int) (((currentTimeNano - startTimeNano) * 100) / testDurationNano);
                        final double reportSpeed = displaySpeed;
                        mainHandler.post(() -> callback.onUploadProgress(reportSpeed, Math.min(progress, 100)));
                        lastReportTimeNano = currentTimeNano;
                    }
                }

                finalBytesWritten[0] = bytesWritten;
                finalTimeNano[0] = System.nanoTime() - startTimeNano;
            }
        };

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .header("Cache-Control", "no-cache")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (!isCancelled && !e.getMessage().contains("Canceled") && !e.getMessage().contains("Socket closed")) {
                    mainHandler.post(() -> callback.onError("Помилка вивантаження: " + e.getMessage()));
                } else if (!isCancelled) {
                    reportFinalUpload(callback, onComplete, finalBytesWritten[0], finalTimeNano[0]);
                }
            }

            @Override
            public void onResponse(Call call, Response response) {
                response.close();
                reportFinalUpload(callback, onComplete, finalBytesWritten[0], finalTimeNano[0]);
            }
        });
    }

    private void reportFinalUpload(SpeedTestCallback callback, Runnable onComplete, long bytes, long timeNano) {
        if (timeNano == 0) timeNano = 1;
        double exactFinalSpeed = ((bytes * 8.0) / 1_000_000.0) / (timeNano / 1_000_000_000.0);
        mainHandler.post(() -> {
            callback.onUploadFinished(exactFinalSpeed);
            onComplete.run();
        });
    }
}