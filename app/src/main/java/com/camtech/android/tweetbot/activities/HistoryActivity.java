package com.camtech.android.tweetbot.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
    private AlertDialog sortDialog;
    private String orderBy = DbUtils.DEFAULT_SORT;
    // Used to keep track of the current radio button selected
    private int itemSelected = 0;

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
            viewAdapter = new HistoryViewAdapter(this, pairs);
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
            case R.id.sort_order:
                changeSortOrder();
                break;
        }
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Start the GraphActivity when the device is rotated horizontally
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            startActivity(new Intent(this, GraphActivity.class).putExtra("sort", orderBy));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (clearHistoryDialog != null) clearHistoryDialog.dismiss();
        if (deleteWordDialog != null) deleteWordDialog.dismiss();
        if (sortDialog != null) sortDialog.dismiss();
    }

    @Override
    public void onItemDeleted(@NonNull Pair<String, Integer> pair, int position) {
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
        builder.setCancelable(false)
                .setTitle("Clear History")
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

    private void changeSortOrder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String[] items = {
                "Most recent",
                "Keyword ascending",
                "Keyword descending",
                "Occurrences ascending",
                "Occurrences descending"
        };
        builder.setTitle("Sort by")
                .setCancelable(false)
                .setSingleChoiceItems(items, itemSelected, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            itemSelected = 0;
                            orderBy = DbUtils.DEFAULT_SORT;
                            break;
                        case 1:
                            itemSelected = 1;
                            orderBy = DbUtils.KEYWORD_ASC;
                            break;
                        case 2:
                            itemSelected = 2;
                            orderBy = DbUtils.KEYWORD_DESC;
                            break;
                        case 3:
                            itemSelected = 3;
                            orderBy = DbUtils.OCCURRENCES_ASC;
                            break;
                        case 4:
                            itemSelected = 4;
                            orderBy = DbUtils.OCCURRENCES_DESC;
                            break;
                    }
                    pairs = DbUtils.getAllKeyWords(this, orderBy);
                    viewAdapter.resetAdapter(pairs);
                    dialog.dismiss();
                })
                .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        sortDialog = builder.create();
        sortDialog.show();
    }
}
