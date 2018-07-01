package com.camtech.android.tweetbot.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.adapters.HistoryViewAdapter;
import com.camtech.android.tweetbot.utils.DbUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HistoryActivity extends AppCompatActivity implements HistoryViewAdapter.OnItemClickListener {

    private List<Pair<String, Integer>> pairs;
    private HistoryViewAdapter viewAdapter;
    private AlertDialog deleteWordDialog;
    private AlertDialog clearHistoryDialog;

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

        LinearLayoutManager manager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(manager);
        recyclerView.setHasFixedSize(true);

        pairs = DbUtils.getAllKeyWords(this, null);
        if (pairs != null && !pairs.isEmpty()) {
            tvNoHistory.setVisibility(View.GONE);
            viewAdapter = new HistoryViewAdapter(this, this, pairs);
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
                if (pairs != null && !pairs.isEmpty()) {
                    clearHistory();
                }
                break;
        }
        return true;
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
    protected void onStop() {
        super.onStop();
        if (clearHistoryDialog != null) clearHistoryDialog.dismiss();
        if (deleteWordDialog != null) deleteWordDialog.dismiss();
    }

    @Override
    public void onClick(Pair<String, Integer> pair, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Keyword")
                .setMessage("Are you sure you want to delete this word ?")
                .setPositiveButton("OK", ((dialog, which) -> {
                    DbUtils.deleteKeyWord(this, pair.first);
                    viewAdapter.removeCard(position);
                    if (viewAdapter.getItemCount() == 0) {
                        tvNoHistory.setVisibility(View.VISIBLE);
                    }
                }))
                .setNegativeButton("CANCEL", ((dialog, which) -> dialog.dismiss()));
        deleteWordDialog = builder.create();
        deleteWordDialog.show();
    }

    private void clearHistory() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Clear History")
                .setMessage("Are you want to clear your history? This can't be undone!")
                .setPositiveButton("YES", (dialog, which) -> {
                    DbUtils.deleteAllKeyWords(this);
                    pairs.clear();
                    viewAdapter.resetAdapter(pairs);
                    tvNoHistory.setVisibility(View.VISIBLE);
                })
                .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        clearHistoryDialog = builder.create();
        clearHistoryDialog.show();
    }
}
