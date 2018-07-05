package com.camtech.android.tweetbot.activities;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.utils.DbUtils;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;

public class GraphActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        String sortOrder = getIntent().getStringExtra("sort");

        ImageView emptyGraphImage = findViewById(R.id.graph_icon);
        TextView emptyGraphText = findViewById(R.id.graph_text);
        BarChart chart = findViewById(R.id.chart);

        // Hide the notification bar and nav buttons
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        List<Pair<String, Integer>> pairs = DbUtils.getAllKeyWords(this, sortOrder);
        if (pairs == null) {
            // There's no data, so show the empty view
            emptyGraphImage.setVisibility(View.VISIBLE);
            emptyGraphText.setVisibility(View.VISIBLE);
            chart.setVisibility(View.GONE);
        } else {
            emptyGraphImage.setVisibility(View.INVISIBLE);
            emptyGraphText.setVisibility(View.INVISIBLE);

            String[] keyWord = new String[pairs.size()];
            ArrayList<BarEntry> entries = new ArrayList<>();

            int index = 0;
            for (Pair<String, Integer> pair : pairs) {
                // Add each keyword and the number of occurrences to the bar graph.
                // The keyword will be displayed underneath each bar and the
                // number of occurrences will be displayed on top.
                keyWord[index] = pair.first;
                entries.add(new BarEntry(index, pair.second != null ? pair.second : 0));
                index++;
            }

            BarDataSet barDataSet = new BarDataSet(entries, "Occurrences");
            barDataSet.setColor(getResources().getColor(R.color.colorOccurrences));

            BarData barData = new BarData(barDataSet);
            barData.setBarWidth(0.50f);
            final float TEXT_SIZE = 14f;
            barData.setValueTextSize(TEXT_SIZE);
            barData.setValueFormatter((value1, entry, dataSetIndex, viewPortHandler) -> String.valueOf((int) value1));

            XAxis xAxis = chart.getXAxis();
            xAxis.setGranularity(1f);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setTextSize(TEXT_SIZE);
            xAxis.setCenterAxisLabels(false);
            xAxis.setValueFormatter(new IndexAxisValueFormatter(keyWord));

            //--Don't want to display any y axis--//
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
            chart.animateXY(3000, 3000);
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
