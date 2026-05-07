package com.test.myproject.signal.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class NetworkAnalyzer {
    private final Context context;
    private ConnectivityManager.NetworkCallback networkCallback;

    public interface NetworkChangeListener {
        void onNetworkChanged(String networkInfo);
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

    public String getIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();

                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "Невідомо";
    }

    public String getNetworkName() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return "";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return "";
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            if (capabilities == null) return "";

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wifiManager != null) {
                    WifiInfo info = wifiManager.getConnectionInfo();
                    if (info != null && info.getSSID() != null && !info.getSSID().equals(WifiManager.UNKNOWN_SSID)) {
                        String ssid = info.getSSID().replace("\"", "");
                        if (!ssid.equals("<unknown ssid>")) {
                            return ssid;
                        }
                    }
                }
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (telephonyManager != null) {
                    String operatorName = telephonyManager.getNetworkOperatorName();
                    if (operatorName != null && !operatorName.isEmpty()) {
                        return operatorName;
                    }
                }
            }
        }
        return "";
    }

    public String getFullNetworkInfo() {
        String type = getNetworkType();
        if (type.equals("Немає підключення") || type.equals("Невідомо")) return type;

        String name = getNetworkName();
        String ip = getIpAddress();

        StringBuilder sb = new StringBuilder(type);
        if (!name.isEmpty()) {
            sb.append(" (").append(name).append(")");
        }
        if (!ip.equals("Невідомо")) {
            sb.append("\nIP: ").append(ip);
        }
        return sb.toString();
    }

    public void startMonitoring(NetworkChangeListener listener) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    listener.onNetworkChanged(getFullNetworkInfo());
                }

                @Override
                public void onLost(@NonNull Network network) {
                    listener.onNetworkChanged("Немає підключення");
                }

                @Override
                public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                    listener.onNetworkChanged(getFullNetworkInfo());
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

    public String generateConclusion(double downloadMbps, double uploadMbps, long pingMs) {
        StringBuilder conclusion = new StringBuilder();

        if (downloadMbps > 50 && pingMs < 50) {
            conclusion.append("З'єднання відмінне. Підійде для перегляду 4K-відео, онлайн-ігор та відеоконференцій без затримок.");
        } else if (downloadMbps > 15) {
            conclusion.append("Гарне з'єднання. Достатньо для перегляду HD-відео, серфінгу та соцмереж.");
        } else if (downloadMbps > 5) {
            conclusion.append("Добре з'єднання. Можливі затримки при перегляді довгих відео, але для читання новин вистачить.");
        } else {
            conclusion.append("Слабке з'єднання. Інтернет працює дуже повільно, можливі розриви зв'язку.");
        }

        if (uploadMbps < 2) {
            conclusion.append(" Проте, вивантаження файлів буде займати багато часу (відправка великих фото/відео).");
        }

        return conclusion.toString();
    }
}