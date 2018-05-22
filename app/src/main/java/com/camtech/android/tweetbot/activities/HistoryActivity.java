package com.camtech.android.tweetbot.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.camtech.android.tweetbot.adapters.HistoryViewAdapter;
import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.tweet.TwitterUtils;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HistoryActivity extends AppCompatActivity implements HistoryViewAdapter.ClickListener {

    private String[] keyWord;
    private int[] value;
    private HashMap<String, Integer> hashMap;
    private TwitterUtils utils;
    private HistoryViewAdapter viewAdapter;

    @BindView(R.id.tv_no_history) TextView tvNoHistory;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.rv_history) RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitle("History");
        toolbar.setTitleTextColor(Color.WHITE);

        utils = new TwitterUtils();

        LinearLayoutManager manager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(manager);
        recyclerView.setHasFixedSize(true);

        // Get the stored HashMap from memory if it exists
        // and save the key-value pairs to an array. This is so that
        // the keyword and it's number of occurrences can be displayed
        // by the OccurrencesFragment when a word is clicked
        hashMap = utils.getHashMap();
        if (hashMap != null && !hashMap.isEmpty()) {
            tvNoHistory.setVisibility(View.GONE);
            keyWord = new String[hashMap.entrySet().size()];
            value = new int[hashMap.keySet().size()];
            int index = 0;
            for (Map.Entry<String, Integer> map : hashMap.entrySet()) {
                keyWord[index] = map.getKey();
                value[index] = map.getValue();
                index++;
            }
            viewAdapter = new HistoryViewAdapter(this, hashMap, this);
            recyclerView.setAdapter(viewAdapter);
        } else {
            tvNoHistory.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.clear_history:
                clearHistory();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Start the GraphActivity when the device is rotated horizontally
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

    private void clearHistory() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Clear History");
        builder.setMessage("Are you want to clear your history? This can't be undone!");
        builder.setPositiveButton("YES", (dialog, which) -> {
            hashMap.clear();
            utils.saveHashMap(hashMap);
            viewAdapter.resetAdapter(hashMap);
            tvNoHistory.setVisibility(View.VISIBLE);
        });
        builder.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
}
