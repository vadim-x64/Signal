package com.test.myproject.signal.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.test.myproject.signal.R;
import com.test.myproject.signal.data.SessionData;
import com.test.myproject.signal.data.TestResult;

import java.util.ArrayList;
import java.util.List;

public class ChartsActivity extends AppCompatActivity {
    private LineChart chartDownload;
    private BarChart chartUpload;
    private TextView tvNoData;
    private Group chartsGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charts);
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        chartDownload = findViewById(R.id.chartDownload);
        chartUpload = findViewById(R.id.chartUpload);
        tvNoData = findViewById(R.id.tvNoData);
        chartsGroup = findViewById(R.id.chartsGroup);
        loadChartsData();
    }

    private void loadChartsData() {
        List<TestResult> history = SessionData.getInstance().getHistory();

        if (history.isEmpty()) {
            chartsGroup.setVisibility(View.GONE);
            tvNoData.setVisibility(View.VISIBLE);
            return;
        }

        chartsGroup.setVisibility(View.VISIBLE);
        tvNoData.setVisibility(View.GONE);
        setupLineChart(history);
        setupBarChart(history);
    }

    private void setupLineChart(List<TestResult> history) {
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < history.size(); i++) {
            entries.add(new Entry(i, (float) history.get(i).getDownloadSpeed()));
            labels.add(history.get(i).getFormattedTime());
        }

        LineDataSet dataSet = new LineDataSet(entries, "Завантаження (Мбіт/с)");
        dataSet.setColor(Color.parseColor("#92FE9D"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleColor(Color.WHITE);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawValues(true);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#92FE9D"));
        dataSet.setFillAlpha(50);
        LineData lineData = new LineData(dataSet);
        chartDownload.setData(lineData);
        chartDownload.getDescription().setEnabled(false);
        chartDownload.getLegend().setTextColor(Color.WHITE);
        chartDownload.getAxisRight().setEnabled(false);
        chartDownload.getAxisLeft().setTextColor(Color.WHITE);
        chartDownload.getAxisLeft().setAxisMinimum(0f);
        XAxis xAxis = chartDownload.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        chartDownload.animateX(1000);
        chartDownload.invalidate();
    }

    private void setupBarChart(List<TestResult> history) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < history.size(); i++) {
            entries.add(new BarEntry(i, (float) history.get(i).getUploadSpeed()));
            labels.add(history.get(i).getFormattedTime());
        }

        BarDataSet dataSet = new BarDataSet(entries, "Вивантаження (Мбіт/с)");
        dataSet.setColor(Color.parseColor("#00C9FF"));
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        chartUpload.setData(barData);
        chartUpload.getDescription().setEnabled(false);
        chartUpload.getLegend().setTextColor(Color.WHITE);
        chartUpload.getAxisRight().setEnabled(false);
        chartUpload.getAxisLeft().setTextColor(Color.WHITE);
        chartUpload.getAxisLeft().setAxisMinimum(0f);
        XAxis xAxis = chartUpload.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        chartUpload.animateY(1000);
        chartUpload.invalidate();
    }
}