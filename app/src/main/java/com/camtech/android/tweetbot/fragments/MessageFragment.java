package com.camtech.android.tweetbot.fragments;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.tweet.StreamListener;
import com.camtech.android.tweetbot.tweet.TwitterService;
import com.camtech.android.tweetbot.tweet.TwitterUtils;

import twitter4j.DirectMessage;
import twitter4j.TwitterException;

/**
 * This fragment is responsible for starting the messaging
 * part of the stream listener.
 *
 * More specifically, see {@link StreamListener#onDirectMessage(DirectMessage)}
 * */
public class MessageFragment extends Fragment {

    private Button startStop;
    private TextView runningStatus;
    private TextView tvUserName;

    public static final String MESSAGE = "Message";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.message_fragment, container, false);

        tvUserName = rootView.findViewById(R.id.tv_username);
        tvUserName.setText(R.string.username_loading);

        runningStatus = rootView.findViewById(R.id.tv_running_status);
        startStop = rootView.findViewById(R.id.bt_start_stop);
        startStop.setOnClickListener(view -> {
            vibrate(30);
            if (!isServiceRunning(TwitterService.class)) {
                Intent intent = new Intent(getContext(), TwitterService.class);
                // Tell the service that it needs to use the 'user' stream
                intent.putExtra(Intent.EXTRA_TEXT, MESSAGE);
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
    public void onPause() {
        super.onPause();
        getContext().unregisterReceiver(updateButtonReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Receiver to make sure text updates
        getContext().registerReceiver(updateButtonReceiver, new IntentFilter(TwitterService.BROADCAST_UPDATE));

        // Once the async task has completed we can set the username to the resulted string
        ConnectionUtils.OnPostExecuteListener listener =
                result -> tvUserName.setText(getString(R.string.status_user, result));
        new ConnectionUtils(listener).execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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

    /**
     * When the service starts/stops, we need to update
     * the text of the button
     * */
    private void updateButtonText() {
        if (isServiceRunning(TwitterService.class)) {
            startStop.setText(R.string.button_stop);
            runningStatus.setText(R.string.status_running);
        } else {
            startStop.setText(R.string.button_start);
            runningStatus.setText("");
        }
    }

    private BroadcastReceiver updateButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateButtonText();
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
     */
    private static class ConnectionUtils extends AsyncTask<Void, Void, String> {
        private String userName;
        private OnPostExecuteListener listener;

        ConnectionUtils(OnPostExecuteListener listener) {
            this.listener = listener;
        }

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
            super.onPostExecute(s);
            listener.onPostExecute(userName);
        }

        interface OnPostExecuteListener {
            void onPostExecute(String result);
        }
    }
}
