package com.camtech.android.tweetbot.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.fragments.FragmentAdapter;
import com.camtech.android.tweetbot.models.Keys;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;

import me.relex.circleindicator.CircleIndicator;

public class MainActivity extends AppCompatActivity {

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
            public void onPageScrolled(int currentPage, float positionOffset, int positionOffsetPixels) {
                // Moving the view the full value of the offset moves it WAY too fast
                // so we'll use half of the value
                int halfOffsetPixels = positionOffsetPixels / 2;
                float currentPositionY = indicator.getTranslationY();
                if (currentPage + positionOffset > sumPositionAndPositionOffset) {
                    // A swipe from right to left, move the view down
                    if (halfOffsetPixels > currentPositionY) {
                        indicator.setTranslationY(halfOffsetPixels);
                    }
                } else {
                    // A swipe from left to right, move the view up.
                    // We also only want to move the view up when going from
                    // the second fragment, to the first fragment
                    if (currentPage < 1) {
                        indicator.setTranslationY((halfOffsetPixels * 2) - halfOffsetPixels);
                    }
                }
                sumPositionAndPositionOffset = currentPage + positionOffset;
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
}
