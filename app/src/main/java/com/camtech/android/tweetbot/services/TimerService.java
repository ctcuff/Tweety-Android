package com.camtech.android.tweetbot.services;

import android.app.Service;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;

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
    private CountDownTimer timer;
    final long RUNTIME = /* 25 seconds */ 25_000L;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Ths intent broadcast to receiving containing the
        // amount of time the timer has left
        Intent timeIntent = new Intent(BROADCAST_TIME);

        timer = new CountDownTimer(RUNTIME, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeIntent.putExtra(INTENT_TIME_LEFT, millisUntilFinished / 1000);
                sendBroadcast(timeIntent);
            }

            @Override
            public void onFinish() {
                timeIntent.putExtra(INTENT_TIME_LEFT, 0L);
                sendBroadcast(timeIntent);
                stopSelf();
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