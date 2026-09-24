package com.alinaj.dev.fragment;

import static android.content.Context.RECEIVER_EXPORTED;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.alinaj.dev.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @developer_ Name: Rubel Brand Name: rksoft, https://t.me/rksoft_update
 * @date 2024/05/11
 */

public class GraphFragment extends Fragment {

    private LineChart mChart;

    private DecimalFormat df;

    private ArrayList<Entry> e1;
    private ArrayList<Entry> e2;
    private List<Long> dList;
    private List<Long> uList;

    private final List<Long> downloadList = new ArrayList<>();
    private final List<Long> uploadList = new ArrayList<>();

    public static final String color_download_graph = "#2963ff";
    public static final String color_graph_text = "#000000";
    public static final String color_upload_graph = "#d50000";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.graph, container, false);

        mChart = v.findViewById(R.id.chart);

        try {
            setGraph();
        } catch (Exception e) {
            log("Exception 1 : " + e);
        }

        log("Graph created");

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity) {

			log("Graph attached");

            if (downloadList.size() == 0) {
                for (int i = 0; i < 150; i++) {
                    downloadList.add(0L);
                }
            }

            if (uploadList.size() == 0) {
                for (int i = 0; i < 150; i++) {
                    uploadList.add(0L);
                }
            }

			Activity activity = (Activity) context;

            BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context arg0, Intent arg1) {

                    downloadList.remove(0);
                    downloadList.add(arg1.getLongExtra("DOWNLOAD", 0));
                    uploadList.remove(0);
                    uploadList.add(arg1.getLongExtra("UPLOAD", 0));

                    if (mChart == null) {
                        return;
                    }

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                addDataSet();
                            } catch (Exception e) {
                                log("Exception 2 : " + e);
                            }
                        }
                    });
                }
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.registerReceiver(broadcastReceiver, new IntentFilter(context.getPackageName() + ".GRAPH"), RECEIVER_EXPORTED);
            } else {
                activity.registerReceiver(broadcastReceiver, new IntentFilter(context.getPackageName() + ".GRAPH"));
            }
        }
    }

    private void setGraph() {

        this.df = new DecimalFormat("#.##");
        this.dList = downloadList;
        this.uList = uploadList;
        this.e1 = new ArrayList<>();
        this.e2 = new ArrayList<>();

        LineDataSet lineDataSet = new LineDataSet(this.e2, "Download");
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineDataSet.setCubicIntensity(0.2f);
        lineDataSet.setDrawFilled(true);
        lineDataSet.setDrawValues(false);
        lineDataSet.setFillColor(Color.parseColor("#ffcdd3"));
        lineDataSet.setFillAlpha(5000);
        lineDataSet.setLineWidth(0.5f);
        lineDataSet.setCircleRadius(0.5f);
        lineDataSet.setDrawValues(false);
        lineDataSet.setColor(Color.parseColor(color_download_graph));
        lineDataSet.setCircleColor(0);
        lineDataSet.setCircleColorHole(0);
        lineDataSet.setDrawCircleHole(false);
        LineDataSet lineDataSet2 = new LineDataSet(this.e1, "Upload");
        lineDataSet2.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineDataSet2.setCubicIntensity(0.2f);
        lineDataSet2.setDrawFilled(true);
        lineDataSet2.setDrawValues(false);
        lineDataSet2.setFillColor(Color.parseColor("#ffcdd3"));
        lineDataSet2.setFillAlpha(5000);
        lineDataSet2.setLineWidth(0.5f);
        lineDataSet2.setCircleRadius(0.5f);
        lineDataSet2.setColor(Color.parseColor(color_upload_graph));
        lineDataSet2.setCircleColor(0);
        lineDataSet2.setCircleColorHole(0);
        lineDataSet2.setHighLightColor(Color.rgb(0, 102, 0));
        lineDataSet2.setDrawValues(false);
        lineDataSet2.setDrawCircleHole(false);

        ArrayList arrayList = new ArrayList();
        arrayList.add(lineDataSet2);
        arrayList.add(lineDataSet);

        XAxis xAxis = this.mChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisLineColor(getResources().getColor(R.color.primary_color));
        this.mChart.getXAxis().setDrawLabels(false);
        this.mChart.getAxisLeft().setDrawGridLines(false);
        this.mChart.getXAxis().setDrawGridLines(false);
        this.mChart.getAxisLeft().setEnabled(false);
        this.mChart.getAxisRight().setEnabled(false);
        this.mChart.setDrawGridBackground(false);
        this.mChart.setGridBackgroundColor(0);
        this.mChart.getLegend().setEnabled(false);
        this.mChart.getDescription().setEnabled(false);
        this.mChart.setTouchEnabled(false);
        this.mChart.setData(new LineData(arrayList));
        this.mChart.getViewPortHandler().setMaximumScaleX(5.0f);
        this.mChart.getViewPortHandler().setMaximumScaleY(5.0f);
        this.mChart.notifyDataSetChanged();
        this.mChart.invalidate();
    }

    public void addDataSet() {
        float f;
        LineData lineData = this.mChart.getData();
        if (lineData != null) {
            this.dList = downloadList;
            this.uList = uploadList;
            this.e1 = new ArrayList<>();
            this.e2 = new ArrayList<>();
            float f2 = 0.0f;
            for (int i = 0; i < this.dList.size(); i++) {
                float longValue = ((float) this.dList.get(i).longValue()) / 1024.0f;
                float longValue2 = ((float) (i >= uList.size() ? this.dList.get(i) : this.uList.get(i)).longValue()) / 1024.0f;
                float f3 = i;
                this.e1.add(new Entry(f3, longValue));
                this.e2.add(new Entry(f3, longValue2));
                if (f2 < longValue) {
                    f2 = longValue;
                }
                if (f2 < longValue2) {
                    f2 = longValue2;
                }
            }
            float f4 = 2048.0f;
            String str = " KB/s";
            if (f2 <= 224.0f) {
                f = f2;
                f4 = 256.0f;
            } else {
                if (f2 <= 256.0f) {
                    f4 = 512.0f;
                } else if (f2 <= 896.0f) {
                    f = f2;
                    f4 = 1024.0f;
                } else if (f2 >= 1024.0f) {
                    if (f2 >= 1792.0f) {
                        f4 = f2 < 3584.0f ? 4096.0f : f2 < 7168.0f ? 8192.0f : f2 < 14336.0f ? 16384.0f : 32768.0f;
                    }
                    f = f2 / 1024.0f;
                    str = " MB/s";
                }
                f = f2;
            }
            LineDataSet lineDataSet = new LineDataSet(this.e2, "Download");
            lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            lineDataSet.setCubicIntensity(0.2f);
            lineDataSet.setDrawFilled(true);
            lineDataSet.setDrawValues(false);
            lineDataSet.setFillColor(Color.parseColor("#ffcdd3"));
            lineDataSet.setFillAlpha(5000);
            lineDataSet.setLineWidth(0.5f);
            lineDataSet.setCircleRadius(0.5f);
            lineDataSet.setDrawValues(false);
            lineDataSet.setColor(Color.parseColor(color_download_graph));
            lineDataSet.setCircleColor(0);
            lineDataSet.setCircleColorHole(0);
            lineDataSet.setDrawCircleHole(false);
            LineDataSet lineDataSet2 = new LineDataSet(this.e1, "Upload");
            lineDataSet2.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            lineDataSet2.setCubicIntensity(0.2f);
            lineDataSet2.setDrawFilled(true);
            lineDataSet2.setDrawValues(false);
            lineDataSet2.setFillColor(Color.parseColor("#ffcdd3"));
            lineDataSet2.setFillAlpha(5000);
            lineDataSet2.setLineWidth(0.5f);
            lineDataSet2.setCircleRadius(0.5f);
            lineDataSet2.setColor(Color.parseColor(color_upload_graph));
            lineDataSet2.setCircleColor(0);
            lineDataSet2.setCircleColorHole(0);
            lineDataSet2.setHighLightColor(Color.rgb(0, 102, 0));
            lineDataSet2.setDrawValues(false);
            lineDataSet2.setDrawCircleHole(false);
            String sb = this.df.format(f) +
                    str;
            LimitLine limitLine = new LimitLine(f2, sb);
            limitLine.setLineWidth(1.0f);
            limitLine.enableDashedLine(10.0f, 10.0f, 0.0f);
            limitLine.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_TOP);
            limitLine.setTextSize(6.0f);
            limitLine.setTextColor(Color.parseColor(color_graph_text));
            limitLine.setLineColor(0);
            limitLine.setTypeface(Typeface.DEFAULT);
            XAxis xAxis = this.mChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(true);
            xAxis.setDrawAxisLine(true);
            xAxis.setLabelCount(0, true);
            xAxis.setTextSize(6.0f);
            xAxis.setAxisMinValue(0.0f);
            xAxis.setDrawLabels(true);
            xAxis.setTypeface(Typeface.DEFAULT);
            xAxis.setTextColor(Color.parseColor(color_graph_text));
            xAxis.setEnabled(false);
            xAxis.enableGridDashedLine(5.0f, 5.0f, 1.0f);
            YAxis axisLeft = this.mChart.getAxisLeft();
            axisLeft.setLabelCount(0, true);
            axisLeft.setAxisMaxValue(f4);
            axisLeft.setAxisMinValue(0.0f);
            axisLeft.enableGridDashedLine(5.0f, 5.0f, 1.0f);
            axisLeft.removeAllLimitLines();
            axisLeft.addLimitLine(limitLine);
            axisLeft.setDrawLimitLinesBehindData(true);
            axisLeft.setTextColor(Color.parseColor(color_graph_text));
            axisLeft.setTextSize(6.0f);
            axisLeft.setEnabled(false);
            this.mChart.getAxisRight().setEnabled(true);
            YAxis axisRight = this.mChart.getAxisRight();
            axisRight.setLabelCount(10, true);
            axisRight.setAxisMaxValue(f4 / 1024.0f);
            axisRight.setAxisMinValue(0.0f);
            axisRight.enableGridDashedLine(5.0f, 5.0f, 1.0f);
            axisRight.setDrawGridLines(false);
            axisRight.setTextSize(6.0f);
            axisRight.setTextColor(Color.parseColor(color_graph_text));
            lineData.removeDataSet(0);
            lineData.removeDataSet(1);
            lineData.clearValues();
            lineData.addDataSet(lineDataSet2);
            lineData.addDataSet(lineDataSet);
            lineData.notifyDataChanged();
            Legend legend = this.mChart.getLegend();
            legend.setTextSize(6.0f);
            legend.setTypeface(Typeface.SERIF);
            legend.setTextColor(0);
            legend.setPosition(Legend.LegendPosition.BELOW_CHART_LEFT);
            legend.setEnabled(false);
            this.mChart.setTouchEnabled(false);
            this.mChart.getDescription().setEnabled(false);
            this.mChart.setData(lineData);
            this.mChart.getViewPortHandler().setMaximumScaleX(5.0f);
            this.mChart.getViewPortHandler().setMaximumScaleY(5.0f);
            this.mChart.notifyDataSetChanged();
            this.mChart.invalidate();
        }
    }

    private void log(String str) {
        Log.d("technore_graph", str);
    }
}