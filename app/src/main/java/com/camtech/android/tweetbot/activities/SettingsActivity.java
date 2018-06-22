package com.camtech.android.tweetbot.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.utils.TwitterUtils;

import twitter4j.Twitter;
import twitter4j.TwitterException;

public class SettingsActivity extends AppCompatActivity {


    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            // If the user is currently logged in, get their screen name from
            // Twitter and set it at the sub title for the action bar
            if (TwitterUtils.isUserLoggedIn()) {
                Twitter twitter = TwitterUtils.getTwitter(this);
                if (twitter != null) {
                    new AsyncTask<Void, Void, String>() {

                        @Override
                        protected String doInBackground(Void... voids) {
                            try {
                                return twitter.getScreenName();
                            } catch (TwitterException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(String screenName) {
                            super.onPostExecute(screenName);
                            if (screenName != null) {
                                actionBar.setSubtitle(getString(R.string.status_user, screenName));
                            }
                        }
                    }.execute();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_settings);
        fragment.onActivityResult(requestCode, resultCode, data);
    }
}
