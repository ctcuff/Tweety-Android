package com.camtech.android.tweetbot.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.camtech.android.tweetbot.core.StreamListener;

/**
 * Since every call to start the {@link StreamListener} authenticates
 * the user, we need to allow a small break in between auths so
 * we don't get slapped in the face with the scary
 * <a href = "https://developer.twitter.com/en/docs/basics/rate-limiting.html">error 420</a>
 *
 * @see <a href="https://developer.twitter.com/en/docs/basics/response-codes.html">Twitter error codes</a>
 */
public class TimerService extends Service {

    private final String TAG = TimerService.class.getSimpleName();

    public static final String BROADCAST_TIME = "Timer";
    public static final String INTENT_TIME_LEFT = "timeLeft";
    public static final String INTENT_EXTRA_TIME = "Time";
    private CountDownTimer timer;
    private SharedPreferences timerPref;
    private final String PREF_TIME_REMAINING = "timeRemaining";
    final long DEFAULT_RUNTIME = /* 30 seconds */ 30_000L;
    final String TIME_PREF = "Time";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        timerPref = getSharedPreferences(TIME_PREF, Context.MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Ths intent broadcast to receiving containing the
        // amount of time the timer has left
        Intent timeIntent = new Intent(BROADCAST_TIME);
        // There might've been an intent passed to this service to
        // use a time other than 30 seconds
        long runtime = intent != null ? intent.getLongExtra(INTENT_EXTRA_TIME, DEFAULT_RUNTIME) : DEFAULT_RUNTIME;
        long timeInPref = timerPref.getLong(PREF_TIME_REMAINING, DEFAULT_RUNTIME);
        timer = new CountDownTimer(timeInPref > runtime ? timeInPref : runtime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.i(TAG, "onTick: Time left: " + millisUntilFinished / 1000);
                timeIntent.putExtra(INTENT_TIME_LEFT, millisUntilFinished / 1000);
                sendBroadcast(timeIntent);
                timerPref.edit().putLong(PREF_TIME_REMAINING, millisUntilFinished).apply();
            }

            @Override
            public void onFinish() {
                timeIntent.putExtra(INTENT_TIME_LEFT, 0L);
                sendBroadcast(timeIntent);
                stopSelf();
                timerPref.edit().putLong(PREF_TIME_REMAINING, 0L).apply();
            }
        };
        timer.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timer.cancel();
    }
}