package com.camtech.android.tweetbot.activities;

import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.twitter.TwitterUtils;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GraphActivity extends AppCompatActivity {
    private final String TAG = GraphActivity.class.getSimpleName();
    final float TEXT_SIZE = 14f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        TwitterUtils utils = new TwitterUtils();

        ImageView emptyGraphImage = findViewById(R.id.graph_icon);
        TextView emptyGraphText = findViewById(R.id.graph_text);
        BarChart chart = findViewById(R.id.chart);

        // Hide the notification bar and nav buttons
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // Get the stored HashMap if it exists
        HashMap<String, Integer> hashMap = utils.getHashMap();
        if (hashMap == null) {
            // There's no data, so show the empty view
            emptyGraphImage.setVisibility(View.VISIBLE);
            emptyGraphText.setVisibility(View.VISIBLE);
            chart.setVisibility(View.GONE);
        } else {
            emptyGraphImage.setVisibility(View.INVISIBLE);
            emptyGraphText.setVisibility(View.INVISIBLE);

            String[] keyWord = new String[hashMap.entrySet().size()];
            int[] value = new int[hashMap.keySet().size()];
            ArrayList<BarEntry> entries = new ArrayList<>();

            int index = 0;
            for (Map.Entry<String, Integer> map : hashMap.entrySet()) {
                keyWord[index] = map.getKey();
                value[index] = map.getValue();
                entries.add(new BarEntry(index, value[index]));
                index++;
            }

            BarDataSet barDataSet = new BarDataSet(entries, "Occurrences");
            barDataSet.setColor(getResources().getColor(R.color.colorOccurrences, null));

            BarData barData = new BarData(barDataSet);
            barData.setBarWidth(0.50f);
            barData.setValueTextSize(TEXT_SIZE);
            barData.setValueFormatter(new IValueFormatter() {
                @Override
                public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                    return "" + ((int) value);
                }
            });

            XAxis xAxis = chart.getXAxis();
            xAxis.setGranularity(1f);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setTextSize(TEXT_SIZE);
            xAxis.setCenterAxisLabels(false);
            xAxis.setValueFormatter(new IndexAxisValueFormatter(keyWord));
            //--Don't wanna display any y axis--//
            YAxis yAxisRight = chart.getAxisRight();
            yAxisRight.setEnabled(false);
            YAxis yAxisLeft = chart.getAxisLeft();
            yAxisLeft.setEnabled(false);
            //-----------------------------------//

            Description desc = new Description();
            desc.setText(" ");

            chart.setData(barData);
            chart.getAxisLeft().setDrawGridLines(false);
            chart.getXAxis().setDrawGridLines(false);
            chart.getXAxis().setDrawAxisLine(true);
            chart.getAxisLeft().setAxisMinimum(0f);
            chart.zoom(1.5f, 0f, 0, 0);
            chart.setScaleYEnabled(false); // Disable vertical zoom
            chart.setDoubleTapToZoomEnabled(false);
            chart.setDescription(desc);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Rotate your device to go back", Toast.LENGTH_LONG).show();
    }
}
