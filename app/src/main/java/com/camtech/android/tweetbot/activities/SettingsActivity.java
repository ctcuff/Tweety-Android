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
import com.camtech.android.tweetbot.models.Keys;
import com.camtech.android.tweetbot.utils.TwitterUtils;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;

import twitter4j.TwitterException;

public class SettingsActivity extends AppCompatActivity {

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TwitterAuthConfig authConfig = new TwitterAuthConfig(
                Keys.CONSUMER_KEY,
                Keys.CONSUMER_KEY_SECRET);
        TwitterConfig twitterConfig = new TwitterConfig.Builder(this)
                .debug(false)
                .twitterAuthConfig(authConfig)
                .build();
        Twitter.initialize(twitterConfig);
        setContentView(R.layout.activity_settings);
        setActionBarSubtitle();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_settings);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void setActionBarSubtitle() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            // If the user is currently logged in, get their screen name from
            // Twitter and set it at the subtitle for the action bar. This requires
            // networking so we have to do it on a different thread
            if (TwitterUtils.isUserLoggedIn()) {
                twitter4j.Twitter twitter = TwitterUtils.getTwitter(this);
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
            } else {
                actionBar.setSubtitle(getString(R.string.not_logged_in_message));
            }
        }
    }
}
