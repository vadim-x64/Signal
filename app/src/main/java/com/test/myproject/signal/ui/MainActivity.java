package com.test.myproject.signal.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.test.myproject.signal.R;
import com.test.myproject.signal.data.SessionData;
import com.test.myproject.signal.data.TestResult;
import com.test.myproject.signal.network.NetworkAnalyzer;
import com.test.myproject.signal.network.SpeedTester;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextView tvNetworkType, tvCurrentSpeed, tvTestState, tvSpeedUnit;
    private TextView tvPing, tvDownload, tvUpload, tvDuration, tvConclusion, tvDateTime;
    private TextView tvServerLocation;
    private ProgressBar progressBarTest;
    private ImageView btnHistory, btnRefresh;
    private LiquidButtonView btnStartTest;
    private SpeedometerView speedometerView;
    private ConnectionFlowView connectionFlowView;
    private GridLayout gridResults;
    private NetworkAnalyzer networkAnalyzer;
    private SpeedTester speedTester;

    private boolean isTesting = false;
    private Handler clockHandler;
    private Runnable clockRunnable;
    private Handler durationHandler;
    private Runnable durationRunnable;
    private long testStartTime = 0;
    private double finalDownload = 0, finalUpload = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
        }

        initViews();
        startClockWidget();
        networkAnalyzer = new NetworkAnalyzer(this);
        speedTester = new SpeedTester();

        networkAnalyzer.startMonitoring(networkInfo -> {
            runOnUiThread(() -> {
                tvNetworkType.setText("Підключення: " + networkInfo);
                tvNetworkType.setAlpha(0.3f);
                tvNetworkType.animate().alpha(1f).setDuration(800).start();
            });
        });

        updateNetworkInfo();

        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ChartsActivity.class));
        });

        btnRefresh.setOnClickListener(v -> {
            btnRefresh.animate().rotationBy(360f).setDuration(500).start();
            resetUI();
            Toast.makeText(this, "Дані оновлено", Toast.LENGTH_SHORT).show();
        });

        btnStartTest.setOnClickListener(v -> {
            btnStartTest.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction(() -> {
                btnStartTest.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                if (isTesting) {
                    cancelTest();
                } else {
                    startTest();
                }
            }).start();
        });

        gridResults.setAlpha(0f);
        gridResults.animate().alpha(1f).setDuration(1500).start();
        connectionFlowView.setState(ConnectionFlowView.State.IDLE);
    }

    private void initViews() {
        tvDateTime = findViewById(R.id.tvDateTime);
        btnHistory = findViewById(R.id.btnHistory);
        btnRefresh = findViewById(R.id.btnRefresh);
        tvNetworkType = findViewById(R.id.tvNetworkType);
        tvCurrentSpeed = findViewById(R.id.tvCurrentSpeed);
        tvSpeedUnit = findViewById(R.id.tvSpeedUnit);
        tvTestState = findViewById(R.id.tvTestState);
        progressBarTest = findViewById(R.id.progressBarTest);
        tvPing = findViewById(R.id.tvPing);
        tvDownload = findViewById(R.id.tvDownload);
        tvUpload = findViewById(R.id.tvUpload);
        tvDuration = findViewById(R.id.tvDuration);
        tvConclusion = findViewById(R.id.tvConclusion);
        btnStartTest = findViewById(R.id.btnStartTest);
        speedometerView = findViewById(R.id.speedometerView);
        gridResults = findViewById(R.id.gridResults);

        connectionFlowView = findViewById(R.id.connectionFlowView);
        tvServerLocation = findViewById(R.id.tvServerLocation);

        durationHandler = new Handler(Looper.getMainLooper());

        durationRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTesting && testStartTime > 0) {
                    double seconds = (System.currentTimeMillis() - testStartTime) / 1000.0;
                    tvDuration.setText(String.format(Locale.US, "%.1f с", seconds));
                    durationHandler.postDelayed(this, 100);
                }
            }
        };
    }

    private void startClockWidget() {
        clockHandler = new Handler(Looper.getMainLooper());
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss\ndd.MM.yyyy", new Locale("uk", "UA"));

        clockRunnable = new Runnable() {
            @Override
            public void run() {
                tvDateTime.setText(sdf.format(new Date()));
                clockHandler.postDelayed(this, 1000);
            }
        };

        clockHandler.post(clockRunnable);
    }

    private void updateNetworkInfo() {
        tvNetworkType.setText("Підключення: " + networkAnalyzer.getFullNetworkInfo());
    }

    private void resetUI() {
        if (isTesting) {
            cancelTest();
        }

        tvPing.setText("-- мс");
        tvDownload.setText("-- Мбіт/с");
        tvUpload.setText("-- Мбіт/с");
        tvDuration.setText("0.0 с");
        tvCurrentSpeed.setText("0.0");
        tvCurrentSpeed.setAlpha(1f);
        tvSpeedUnit.setText("Мбіт/с");
        tvTestState.setText("Очікування");
        tvConclusion.setText("Очікування початку тестування...");
        tvServerLocation.setText("Сервер: Очікування...");
        tvServerLocation.setVisibility(View.GONE);
        connectionFlowView.setVisibility(View.GONE);
        connectionFlowView.setState(ConnectionFlowView.State.IDLE);
        progressBarTest.setProgress(0);
        speedometerView.reset();
        updateNetworkInfo();
    }

    private void startTest() {
        isTesting = true;
        btnStartTest.setTestingState(true);
        gridResults.animate().alpha(0.4f).setDuration(600).start();
        tvConclusion.animate().alpha(0.4f).setDuration(600).start();
        tvPing.setText("-- мс");
        tvDownload.setText("-- Мбіт/с");
        tvUpload.setText("-- Мбіт/с");
        tvDuration.setText("0.0 с");
        tvCurrentSpeed.setText("0.0");
        tvCurrentSpeed.setAlpha(1f);
        tvSpeedUnit.setText("Мбіт/с");
        progressBarTest.setProgress(0);

        tvServerLocation.setText("Пошук оптимального сервера...");
        tvServerLocation.setVisibility(View.VISIBLE);
        connectionFlowView.setVisibility(View.VISIBLE);
        connectionFlowView.setAlpha(0f);
        connectionFlowView.animate().alpha(1f).setDuration(500).start();

        tvConclusion.setText("Тестування...");
        testStartTime = System.currentTimeMillis();
        durationHandler.post(durationRunnable);
        speedometerView.reset();
        updateNetworkInfo();

        speedTester.startFullTest(new SpeedTester.SpeedTestCallback() {
            @Override
            public void onServerInfo(String location, String ip) {
                tvServerLocation.setText("Сервер: " + location + " (IP: " + ip + ")");
            }

            @Override
            public void onHandshakeStart() {
                tvTestState.setText("Встановлення з'єднання...");
                connectionFlowView.setState(ConnectionFlowView.State.HANDSHAKE);
            }

            @Override
            public void onPingResult(long pingMs) {
                tvPing.setText(pingMs + " мс");
            }

            @Override
            public void onDownloadProgress(double mbps, int progressPercent) {
                tvTestState.setText("Перевірка завантаження...");
                connectionFlowView.setState(ConnectionFlowView.State.DOWNLOADING);
                tvSpeedUnit.setText("Мбіт/с");
                tvCurrentSpeed.setText(String.format(Locale.US, "%.1f", mbps));
                speedometerView.setSpeed((float) mbps);
                progressBarTest.setProgress(progressPercent / 2);
            }

            @Override
            public void onDownloadFinished(double finalMbps) {
                finalDownload = finalMbps;
                tvDownload.setText(String.format(Locale.US, "%.1f Мбіт/с", finalMbps));
                speedometerView.smoothReset();

                tvCurrentSpeed.animate().alpha(0f).setDuration(400).withEndAction(() -> {
                    tvCurrentSpeed.setText("0.0");
                }).start();
            }

            @Override
            public void onUploadProgress(double mbps, int progressPercent) {
                tvTestState.setText("Перевірка вивантаження...");
                connectionFlowView.setState(ConnectionFlowView.State.UPLOADING);
                tvSpeedUnit.setText("Мбіт/с");
                tvCurrentSpeed.setAlpha(1f);
                tvCurrentSpeed.setText(String.format(Locale.US, "%.1f", mbps));
                speedometerView.setSpeed((float) mbps);
                progressBarTest.setProgress(50 + (progressPercent / 2));
            }

            @Override
            public void onUploadFinished(double finalMbps) {
                finalUpload = finalMbps;
                tvUpload.setText(String.format(Locale.US, "%.1f Мбіт/с", finalMbps));
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                finishTestUI();
                tvConclusion.setText("Тест перервано через помилку.");
            }

            @Override
            public void onFinished(long totalDurationMs) {
                double seconds = totalDurationMs / 1000.0;
                tvDuration.setText(String.format(Locale.US, "%.1f с", seconds));

                long ping = tvPing.getText().toString().contains("--") ? 100 : Long.parseLong(tvPing.getText().toString().split(" ")[0]);
                tvConclusion.setText(networkAnalyzer.generateConclusion(finalDownload, finalUpload, ping));

                SessionData.getInstance().addResult(new TestResult(finalDownload, finalUpload, ping));
                finishTestUI();
            }
        });
    }

    private void cancelTest() {
        speedTester.cancelTest();
        finishTestUI();
        tvConclusion.setText("Тест скасовано користувачем.");
    }

    private void finishTestUI() {
        isTesting = false;
        btnStartTest.setTestingState(false);
        durationHandler.removeCallbacks(durationRunnable);
        tvTestState.setText("Завершено");
        connectionFlowView.setState(ConnectionFlowView.State.IDLE);
        progressBarTest.setProgress(100);
        speedometerView.smoothReset();

        tvCurrentSpeed.animate().alpha(0f).setDuration(400).withEndAction(() -> {
            tvCurrentSpeed.setText("0.0");
            tvCurrentSpeed.animate().alpha(1f).setDuration(400).start();
        }).start();

        tvSpeedUnit.setText("Мбіт/с");
        gridResults.animate().alpha(1f).setDuration(1000).start();
        tvConclusion.setAlpha(0f);
        tvConclusion.animate().alpha(1f).setDuration(1000).start();

        connectionFlowView.animate().alpha(0f).setDuration(1000).withEndAction(() -> {
            connectionFlowView.setVisibility(View.GONE);
            tvServerLocation.setVisibility(View.GONE);
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clockHandler != null) clockHandler.removeCallbacks(clockRunnable);
        if (durationHandler != null) durationHandler.removeCallbacks(durationRunnable);
        if (networkAnalyzer != null) networkAnalyzer.stopMonitoring();
    }
}