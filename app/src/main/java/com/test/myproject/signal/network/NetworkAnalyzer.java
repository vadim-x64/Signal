package com.test.myproject.signal.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import androidx.annotation.NonNull;

public class NetworkAnalyzer {

    private final Context context;
    private ConnectivityManager.NetworkCallback networkCallback;

    public interface NetworkChangeListener {
        void onNetworkChanged(String networkType);
    }

    public NetworkAnalyzer(Context context) {
        this.context = context;
    }

    public String getNetworkType() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return "Невідомо";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network network = connectivityManager.getActiveNetwork();
            if (network == null) return "Немає підключення";
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) return "Невідомо";

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "Wi-Fi";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return "Мобільна мережа";
            }
        }
        return "Невідомо";
    }

    // Динамічний моніторинг стану мережі
    public void startMonitoring(NetworkChangeListener listener) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    listener.onNetworkChanged(getNetworkType());
                }

                @Override
                public void onLost(@NonNull Network network) {
                    listener.onNetworkChanged("Немає підключення");
                }

                @Override
                public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                    listener.onNetworkChanged(getNetworkType());
                }
            };
            cm.registerDefaultNetworkCallback(networkCallback);
        }
    }

    public void stopMonitoring() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null && networkCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cm.unregisterNetworkCallback(networkCallback);
        }
    }

    // Аналіз результатів тесту
    public String generateConclusion(double downloadMbps, double uploadMbps, long pingMs) {
        StringBuilder conclusion = new StringBuilder("Висновок: ");

        if (downloadMbps > 50 && pingMs < 50) {
            conclusion.append("Відмінне з'єднання. Ідеально підходить для 4K відео, онлайн-ігор та відеоконференцій без затримок.");
        } else if (downloadMbps > 15) {
            conclusion.append("Гарне з'єднання. Достатньо для перегляду HD відео, серфінгу та соцмереж.");
        } else if (downloadMbps > 5) {
            conclusion.append("Посереднє з'єднання. Можливі затримки при перегляді важких відео, але для читання новин вистачить.");
        } else {
            conclusion.append("Слабке з'єднання. Інтернет працює дуже повільно, можливі розриви зв'язку.");
        }

        if (uploadMbps < 2) {
            conclusion.append(" Проте, вивантаження файлів буде займати багато часу (відправка великих фото/відео).");
        }

        return conclusion.toString();
    }
}