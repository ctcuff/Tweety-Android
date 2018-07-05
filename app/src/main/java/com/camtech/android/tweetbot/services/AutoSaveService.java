package com.camtech.android.tweetbot.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.util.Log;

import com.camtech.android.tweetbot.core.StreamListener;
import com.camtech.android.tweetbot.utils.DbUtils;
import com.camtech.android.tweetbot.utils.ServiceUtils;

/**
 * If the app is in the background for a long period of time,
 * Android may silently kill the {@link TwitterService}. This is a
 * service that automatically saves the keyword and number of occurrences
 * every minute. This way, when Android kill the TwitterService, the last
 * word count is the word count saved in this service instead of 0.
 */
public class AutoSaveService extends Service {

    private final String TAG = AutoSaveService.class.getSimpleName();
    private CountDownTimer timer;
    private int numOccurrences;
    long RUNTIME = /* 60 seconds*/ 60_000L;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(occurrencesReceiver, new IntentFilter(StreamListener.OCCURRENCES_INTENT_FILTER));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String keyWord = intent.getStringExtra(TwitterService.TAG);
        timer = new CountDownTimer(RUNTIME, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                Pair<String, Integer> pair = Pair.create(keyWord, numOccurrences);
                Log.i(TAG, "Saving " + pair.toString());
                // We only want this service to keep running if
                // the TwitterService is running in the background
                if (ServiceUtils.isServiceRunning(getBaseContext(), TwitterService.class)) {
                    DbUtils.saveKeyWord(getBaseContext(), keyWord, numOccurrences);
                    Log.i(TAG, "Restarting...");
                    timer.start();
                }
            }
        }.start();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(occurrencesReceiver);
        timer.cancel();
    }

    private BroadcastReceiver occurrencesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the updated number of occurrences from the stream listener
            numOccurrences = intent.getIntExtra(StreamListener.NUM_OCCURRENCES_EXTRA, 0);
        }
    };
}
