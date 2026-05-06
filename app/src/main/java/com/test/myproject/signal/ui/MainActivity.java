package com.test.myproject.signal.ui;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

// ВАЖЛИВО: Заміни ці імпорти на свої, якщо класи NetworkAnalyzer та SpeedTester лежать в іншому місці
// import com.test.myproject.signal.network.NetworkAnalyzer;
// import com.test.myproject.signal.network.SpeedTester;

import com.test.myproject.signal.R; // Заміни на свій R-клас
import com.test.myproject.signal.network.NetworkAnalyzer;
import com.test.myproject.signal.network.SpeedTester;

public class MainActivity extends AppCompatActivity {

    private TextView tvNetworkType, tvPing, tvDownload;
    private MaterialButton btnStartTest;

    private NetworkAnalyzer networkAnalyzer;
    private SpeedTester speedTester;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Встановлюємо наш макет екрану (EdgeToEdge поки що прибрали для простоти)
        setContentView(R.layout.activity_main);

        // 1. Знаходимо всі елементи інтерфейсу з XML за їхніми ID
        tvNetworkType = findViewById(R.id.tvNetworkType);
        tvPing = findViewById(R.id.tvPing);
        tvDownload = findViewById(R.id.tvDownload);
        btnStartTest = findViewById(R.id.btnStartTest);

        // 2. Ініціалізуємо наші робочі класи
        networkAnalyzer = new NetworkAnalyzer(this);
        speedTester = new SpeedTester();

        // 3. Відображаємо поточну мережу (Wi-Fi або Cellular) при старті
        updateNetworkInfo();

        // 4. Вішаємо слухач на кнопку "GO"
        btnStartTest.setOnClickListener(v -> startSpeedTest());
    }

    private void updateNetworkInfo() {
        String networkType = networkAnalyzer.getNetworkType();
        tvNetworkType.setText("Підключення: " + networkType);
    }

    private void startSpeedTest() {
        // Оновлюємо UI перед початком тесту: блокуємо кнопку, скидаємо старі значення
        btnStartTest.setEnabled(false);
        btnStartTest.setText("...");
        tvPing.setText("-- ms");
        tvDownload.setText("-- Mbps");

        // Оновимо статус мережі про всяк випадок
        updateNetworkInfo();

        // Викликаємо метод з нашого SpeedTester, який ти виділяв
        speedTester.startDownloadTest(new SpeedTester.SpeedTestCallback() {
            @Override
            public void onPingResult(long pingMs) {
                // Встановлюємо результат пінгу
                tvPing.setText(pingMs + " ms");
            }

            @Override
            public void onDownloadResult(double mbps) {
                // Форматуємо результат швидкості до 2 знаків після коми
                String speedStr = String.format("%.2f Mbps", mbps);
                tvDownload.setText(speedStr);
            }

            @Override
            public void onError(String error) {
                // Якщо помилка — показуємо спливаюче повідомлення і розблоковуємо кнопку
                Toast.makeText(MainActivity.this, "Помилка: " + error, Toast.LENGTH_LONG).show();
                resetButton();
            }

            @Override
            public void onFinished() {
                // Повертаємо кнопку в початковий стан
                resetButton();
            }
        });
    }

    private void resetButton() {
        btnStartTest.setEnabled(true);
        btnStartTest.setText("GO");
    }
}