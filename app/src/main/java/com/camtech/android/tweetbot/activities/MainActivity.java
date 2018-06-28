package com.camtech.android.tweetbot.activities;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.core.StreamListener;
import com.camtech.android.tweetbot.fragments.FragmentAdapter;
import com.camtech.android.tweetbot.models.Keys;
import com.camtech.android.tweetbot.services.TimerService;
import com.camtech.android.tweetbot.services.TwitterService;
import com.camtech.android.tweetbot.utils.DbUtils;
import com.camtech.android.tweetbot.utils.ServiceUtils;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;

import me.relex.circleindicator.CircleIndicator;

public class MainActivity extends AppCompatActivity {
    private int wordCountFromBroadcast;
    private String keyWord;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TwitterAuthConfig authConfig = new TwitterAuthConfig(
                Keys.CONSUMER_KEY,
                Keys.CONSUMER_KEY_SECRET);
        TwitterConfig twitterConfig = new TwitterConfig.Builder(this)
                .twitterAuthConfig(authConfig)
                .build();
        Twitter.initialize(twitterConfig);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        // Find the view pager that will allow the user to swipe between fragments
        ViewPager viewPager = findViewById(R.id.viewpager);

        // Create an adapter that knows which fragment should be shown on each page
        FragmentAdapter adapter = new FragmentAdapter(getSupportFragmentManager());

        // Set the adapter onto the view pager
        viewPager.setAdapter(adapter);
        // Since the StatusSearchFragment is the third fragment,
        // having an offscreen page limit of 2 will prevent that
        // fragment from resetting when we move to fragment #1
        viewPager.setOffscreenPageLimit(2);
        CircleIndicator indicator = findViewById(R.id.indicator);
        indicator.setViewPager(viewPager);

        // Hide the circle indicator when the current
        // fragment is the second and third fragments
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            private float sumPositionAndPositionOffset;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // Moving the view the full value of the offset moves it WAY too fast
                // so we'll use half of the value
                int halfOffsetPixels = positionOffsetPixels / 2;
                float currentPositionY = indicator.getTranslationY();
                if (position + positionOffset > sumPositionAndPositionOffset) {
                    // A swipe from right to left, move the view down
                    if (halfOffsetPixels > currentPositionY) {
                        indicator.setTranslationY(halfOffsetPixels);
                    }
                } else {
                    // A swipe from left to right, move the view up.
                    // We also only want to move the view up when going from
                    // the second fragment, to the first fragment
                    if (position < 1) {
                        indicator.setTranslationY(positionOffsetPixels - halfOffsetPixels);
                    }
                }
                sumPositionAndPositionOffset = position + positionOffset;
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        // Hide the status bar and navigation bar if the device has one
        Window window = getWindow();
        window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.setFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.setFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.getDecorView()
                .setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(occurrencesReceiver, new IntentFilter(StreamListener.OCCURRENCES_INTENT_FILTER));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Makes sure to cancel the notification in the event
        // that the app is swiped away
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(TwitterService.ID_STREAM_CONNECTED);
        }
        // Makes sure to save the keyword and its number of occurrences
        // if the service was running while the app was swiped away
        if (ServiceUtils.isServiceRunning(this, TwitterService.class)) {
            DbUtils.addKeyWord(this, keyWord, wordCountFromBroadcast);
        }
        Intent timerIntent = new Intent(this, TimerService.class);
        stopService(timerIntent);
        startService(timerIntent);

        unregisterReceiver(occurrencesReceiver);
    }

    private BroadcastReceiver occurrencesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the updated number of occurrences from the stream listener
            wordCountFromBroadcast = intent.getIntExtra(StreamListener.NUM_OCCURRENCES_EXTRA, 0);
            keyWord = intent.getStringExtra(StreamListener.KEYWORD_BROADCAST_EXTRA);
        }
    };
}
