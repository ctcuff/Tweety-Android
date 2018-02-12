package com.camtech.android.tweetbot.fragments;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.tweet.TwitterService;
import com.camtech.android.tweetbot.tweet.TwitterUtils;

import twitter4j.TwitterException;

public class MessageFragment extends Fragment {
    private final String TAG = MessageFragment.class.getSimpleName();

    Button startStop;
    TextView runningStatus;
    TextView tvUserName;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_message, container, false);
        // Receiver to make sure text updates
        getContext().registerReceiver(updateButtonReceiver, new IntentFilter(TwitterService.BROADCAST_UPDATE));

        tvUserName = rootView.findViewById(R.id.tv_username);
        tvUserName.setText("Loading...");

        runningStatus = rootView.findViewById(R.id.tv_running_status);
        startStop = rootView.findViewById(R.id.bt_start_stop);
        startStop.setOnClickListener(view -> {
            vibrate(30);
            if (!isServiceRunning(TwitterService.class)) {
                Intent intent = new Intent(getContext(), TwitterService.class);
                // Tell the service that it needs to use the 'user' stream
                intent.putExtra(Intent.EXTRA_TEXT, "Message");
                getContext().startService(new Intent(intent));
                updateButtonText();
            } else {
                getContext().stopService(new Intent(getContext(), TwitterService.class));
                updateButtonText();
            }
        });
        updateButtonText();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        new ConnectionUtils().execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(updateButtonReceiver);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateButtonText() {
        if (isServiceRunning(TwitterService.class)) {
            startStop.setText("STOP");
            runningStatus.setText("Running");
        } else {
            startStop.setText("START");
            runningStatus.setText("");
        }
    }

    private BroadcastReceiver updateButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateButtonText();
            Log.i(TAG, "onReceive...");
        }
    };

    private void vibrate(int intensity) {
        Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(intensity);
        }
    }

    /**
     * AsyncTask to load the username of the bot. This is so that
     * the username will update if the bots username ever changes.
     * */
    @SuppressLint("StaticFieldLeak")
    public class ConnectionUtils extends AsyncTask<Void, Void, String> {
        String userName;

        @Override
        protected String doInBackground(Void... voids) {
            try {
                userName = TwitterUtils.setUpTwitter().getScreenName();
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if (userName != null) {
                tvUserName.setText("@" + userName);
            } else {
                tvUserName.setText("");
            }
        }
    }
}
