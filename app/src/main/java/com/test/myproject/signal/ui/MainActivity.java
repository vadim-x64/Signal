package com.test.myproject.signal.ui;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import com.test.myproject.signal.R;
import com.test.myproject.signal.network.NetworkAnalyzer;
import com.test.myproject.signal.network.SpeedTester;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvNetworkType, tvCurrentSpeed, tvTestState;
    private TextView tvPing, tvDownload, tvUpload, tvDuration, tvConclusion, tvDateTime;
    private ProgressBar progressBarTest;
    private Button btnStartTest;
    private BubbleView bubbleView;
    private LineChart speedChart;
    private GridLayout gridResults;

    private NetworkAnalyzer networkAnalyzer;
    private SpeedTester speedTester;

    private boolean isTesting = false;

    // Для графіка та часу
    private ArrayList<Entry> chartEntries;
    private float chartTimeIndex = 0;
    private Handler clockHandler;
    private Runnable clockRunnable;
    private double finalDownload = 0, finalUpload = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupChart();
        startClockWidget();

        networkAnalyzer = new NetworkAnalyzer(this);
        speedTester = new SpeedTester();

        // Підписуємося на динамічну зміну стану мережі
        networkAnalyzer.startMonitoring(networkType -> {
            runOnUiThread(() -> {
                tvNetworkType.setText("Підключення: " + networkType);
                // Плавне моргання тексту при зміні підключення
                tvNetworkType.setAlpha(0.3f);
                tvNetworkType.animate().alpha(1f).setDuration(800).start();
            });
        });
        updateNetworkInfo();

        btnStartTest.setOnClickListener(v -> {
            if (isTesting) {
                cancelTest();
            } else {
                startTest();
            }
        });

        // Плавна початкова поява блоку результатів (Інтерактив)
        gridResults.setAlpha(0f);
        speedChart.setAlpha(0f);
        gridResults.animate().alpha(1f).setDuration(1500).start();
        speedChart.animate().alpha(1f).setDuration(1500).start();
    }

    private void initViews() {
        tvDateTime = findViewById(R.id.tvDateTime);
        tvNetworkType = findViewById(R.id.tvNetworkType);
        tvCurrentSpeed = findViewById(R.id.tvCurrentSpeed);
        tvTestState = findViewById(R.id.tvTestState);
        progressBarTest = findViewById(R.id.progressBarTest);

        tvPing = findViewById(R.id.tvPing);
        tvDownload = findViewById(R.id.tvDownload);
        tvUpload = findViewById(R.id.tvUpload);
        tvDuration = findViewById(R.id.tvDuration);
        tvConclusion = findViewById(R.id.tvConclusion);

        btnStartTest = findViewById(R.id.btnStartTest);
        bubbleView = findViewById(R.id.bubbleView);
        speedChart = findViewById(R.id.speedChart);
        gridResults = findViewById(R.id.gridResults);
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



    private void setupChart() {
        speedChart.getDescription().setEnabled(false);
        speedChart.setTouchEnabled(false);
        speedChart.getLegend().setEnabled(false);
        speedChart.getAxisRight().setEnabled(false);

        XAxis xAxis = speedChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);

        speedChart.getAxisLeft().setTextColor(Color.WHITE);
        speedChart.getAxisLeft().setAxisMinimum(0f);

        chartEntries = new ArrayList<>();
        updateChartData();
    }

    private void updateChartData() {
        LineDataSet dataSet = new LineDataSet(chartEntries, "Швидкість");
        dataSet.setColor(Color.parseColor("#00C9FF"));
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Плавна лінія

        // Градієнт під лінією
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#00C9FF"));
        dataSet.setFillAlpha(50);

        LineData lineData = new LineData(dataSet);
        speedChart.setData(lineData);
        speedChart.invalidate();
    }

    private void addChartEntry(double value) {
        chartEntries.add(new Entry(chartTimeIndex++, (float) value));
        updateChartData();
    }

    private void updateNetworkInfo() {
        tvNetworkType.setText("Підключення: " + networkAnalyzer.getNetworkType());
    }

    private void startTest() {
        isTesting = true;
        bubbleView.startAnimation(); // Запускаємо бульбашки
        btnStartTest.setText("СТОП");

        // Плавне приглушення старих результатів під час нового тесту
        gridResults.animate().alpha(0.4f).setDuration(600).start();
        speedChart.animate().alpha(0.4f).setDuration(600).start();
        tvConclusion.animate().alpha(0.4f).setDuration(600).start();

        // Красива поява лічильника швидкості
        tvCurrentSpeed.setAlpha(0f);
        tvCurrentSpeed.animate().alpha(1f).setDuration(800).start();

        // Скидання UI
        tvPing.setText("-- мс");
        tvDownload.setText("-- Мбіт/с");
        tvUpload.setText("-- Мбіт/с");
        tvDuration.setText("0.0 с");
        tvCurrentSpeed.setText("0.0");
        progressBarTest.setProgress(0);
        tvConclusion.setText("Тестування...");
        chartEntries.clear();
        chartTimeIndex = 0;
        updateChartData();

        updateNetworkInfo();

        speedTester.startFullTest(new SpeedTester.SpeedTestCallback() {
            @Override
            public void onPingResult(long pingMs) {
                tvPing.setText(pingMs + " мс");
            }

            @Override
            public void onDownloadProgress(double mbps, int progressPercent) {
                tvTestState.setText("Перевірка завантаження...");
                tvCurrentSpeed.setText(String.format(Locale.US, "%.1f", mbps));
                progressBarTest.setProgress(progressPercent / 2); // Перші 50% бару
                addChartEntry(mbps);
            }

            @Override
            public void onDownloadFinished(double finalMbps) {
                finalDownload = finalMbps;
                tvDownload.setText(String.format(Locale.US, "%.1f Мбіт/с", finalMbps));
                chartEntries.clear(); // Очищаємо графік для upload
                chartTimeIndex = 0;
            }

            @Override
            public void onUploadProgress(double mbps, int progressPercent) {
                tvTestState.setText("Перевірка вивантаження...");
                tvCurrentSpeed.setText(String.format(Locale.US, "%.1f", mbps));
                progressBarTest.setProgress(50 + (progressPercent / 2)); // Другі 50% бару
                addChartEntry(mbps);
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

                // Генеруємо висновок
                long ping = tvPing.getText().toString().contains("--") ? 100 : Long.parseLong(tvPing.getText().toString().split(" ")[0]);
                tvConclusion.setText(networkAnalyzer.generateConclusion(finalDownload, finalUpload, ping));

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
        bubbleView.stopAnimation(); // Зупиняємо бульбашки
        btnStartTest.setText("СТАРТ");
        tvTestState.setText("Завершено");
        progressBarTest.setProgress(100);
        tvCurrentSpeed.setText("0.0");

        // Плавне повернення фокусу на результати
        gridResults.animate().alpha(1f).setDuration(1000).start();
        speedChart.animate().alpha(1f).setDuration(1000).start();
        tvConclusion.animate().alpha(1f).setDuration(1000).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clockHandler != null) {
            clockHandler.removeCallbacks(clockRunnable);
        }
        if (networkAnalyzer != null) {
            networkAnalyzer.stopMonitoring();
        }
    }
}