package com.camtech.android.tweetbot.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.utils.TwitterUtils;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class SettingsActivity extends AppCompatActivity {

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ActionBar actionBar = getSupportActionBar();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayUseLogoEnabled(true);

            // If the user is currently logged in, get their screen name from
            // Twitter and set it at the sub title for the action bar
            if (auth.getCurrentUser() != null) {
                Twitter twitter = TwitterUtils.getTwitter(this);
                if (twitter != null) {
                    new AsyncTask<Void, Void, String>() {
                        private Bitmap profilePic;

                        @Override
                        protected String doInBackground(Void... voids) {
                            try {
                                User user = twitter.showUser(twitter.getId());
                                URL url = new URL(user.getProfileImageURL());
                                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                connection.setDoInput(true);
                                connection.connect();
                                InputStream input = connection.getInputStream();
                                profilePic = BitmapFactory.decodeStream(input);
                                return twitter.getScreenName();
                            } catch (TwitterException | IOException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(String screenName) {
                            super.onPostExecute(screenName);
                            if (screenName != null) {
                                actionBar.setSubtitle("@" + screenName);
                                // Load the user's profile picture and set it as the
                                // image for the action bar
                            }
                            if (profilePic != null) {
                                // TODO You probably need a custom action bar...
                                Drawable drawable = new BitmapDrawable(getResources(), profilePic);
                                actionBar.setIcon(drawable);
                            }
                        }
                    }.execute();
                }
            } else {
                actionBar.setSubtitle("You are not logged in");
            }
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_settings);
        fragment.onActivityResult(requestCode, resultCode, data);
    }
}
