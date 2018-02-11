package com.camtech.android.tweetbot.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.camtech.android.tweetbot.HistoryViewAdapter;
import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.tweet.TwitterUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity implements HistoryViewAdapter.ClickListener {

    private final String TAG = HistoryActivity.class.getSimpleName();
    String[] keyWord;
    int[] value;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitle("History");
        toolbar.setTitleTextColor(Color.WHITE);

        TextView tvNoHistory = findViewById(R.id.tv_no_history);

        TwitterUtils utils = new TwitterUtils();

        RecyclerView recyclerView = findViewById(R.id.rv_occurrences);
        LinearLayoutManager manager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(manager);
        recyclerView.setHasFixedSize(true);

        HashMap<String, Integer> hashMap = utils.getHashMap();
        if (hashMap != null && !hashMap.isEmpty()) {
            tvNoHistory.setVisibility(View.GONE);
            keyWord = new String[hashMap.entrySet().size()];
            value = new int[hashMap.keySet().size()];
            int index = 0;
            for (Map.Entry<String, Integer> map : hashMap.entrySet()) {
                Log.i(TAG, "Keyword: " + map.getKey() + " | Value: " + map.getValue());
                keyWord[index] = map.getKey();
                value[index] = map.getValue();
                index++;
            }
            Log.i(TAG, "Keyword array: " + Arrays.toString(keyWord));
            Log.i(TAG, "Value array: " + Arrays.toString(value));
            recyclerView.setAdapter(new HistoryViewAdapter(hashMap, this));
        } else {
            tvNoHistory.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            startActivity(new Intent(this, GraphActivity.class));
        }
    }

    @Override
    public void onItemClicked(int position) {
        // Get the key word and value of the clicked card and store it in a preference.
        // This is so that the word can be displayed in the OccurrencesFragment
        SharedPreferences keyWordPref = getSharedPreferences(getString(R.string.pref_keyword), MODE_PRIVATE);
        SharedPreferences numOccurrences = getSharedPreferences(getString(R.string.pref_num_occurrences), MODE_PRIVATE);
        keyWordPref.edit().putString(getString(R.string.pref_keyword), keyWord[position]).apply();
        numOccurrences.edit().putInt(getString(R.string.pref_num_occurrences), value[position]).apply();
        finish();
    }
}
