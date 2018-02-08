package com.camtech.android.tweetbot.fragments;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.activities.HistoryActivity;
import com.camtech.android.tweetbot.utils.StreamListener;
import com.camtech.android.tweetbot.utils.TwitterService;
import com.camtech.android.tweetbot.utils.TwitterUtils;

import twitter4j.Status;

/**
 * Displays the number of occurrences of a given word
 */
public class OccurrencesFragment extends Fragment {

    private final String TAG = TwitterService.class.getSimpleName();
    private static final String PREF_KEYWORD = "prefKeyword";
    private static final String PREF_NUM_OCCURRENCES = "prefOccurrences";
    private static final String DEFAULT_KEYWORD = "hello world";
    int numOccurrences;
    String keyWord;
    Button startStop;
    SharedPreferences keywordPref;
    SharedPreferences numOccurrencesPref;
    TextView tvKeyword;
    TextView tvNumOccurrences;
    TwitterUtils utils;
    AlertDialog dialog;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_occurrences, container, false);

        // Receiver to update tvNumOccurrences
        getContext().registerReceiver(occurrencesReceiver, new IntentFilter(StreamListener.OCCURRENCES_BROADCAST));
        // Receiver to make sure the button text updates
        getContext().registerReceiver(updateButtonReceiver, new IntentFilter(TwitterService.BROADCAST_UPDATE));

        utils = new TwitterUtils();

        keywordPref = getContext().getSharedPreferences(PREF_KEYWORD, Context.MODE_PRIVATE);
        numOccurrencesPref = getContext().getSharedPreferences(PREF_NUM_OCCURRENCES, Context.MODE_PRIVATE);
        keyWord = keywordPref.getString(PREF_KEYWORD, DEFAULT_KEYWORD);
        numOccurrences = numOccurrencesPref.getInt(PREF_NUM_OCCURRENCES, 0);

        startStop = rootView.findViewById(R.id.bt_start_stop);
        startStop.setOnClickListener(v -> {
            vibrate(30);
            if (!isServiceRunning(TwitterService.class)) {
                // Tell the service it was started through the occurrences fragment
                Intent intent = new Intent(getContext(), TwitterService.class);
                intent.putExtra(Intent.EXTRA_TEXT, "Occurrences");
                getContext().startService(intent);
            } else {
                getContext().stopService(new Intent(getContext(), TwitterService.class));
            }
        });

        tvNumOccurrences = rootView.findViewById(R.id.tv_num_occurrences);
        tvNumOccurrences.setText(String.valueOf(numOccurrences));

        tvNumOccurrences.setOnLongClickListener(v -> {
            if (!isServiceRunning(TwitterService.class)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage("Are you sure you want to reset the counter for this word?")
                        .setPositiveButton("YES", (dialog, which) -> {
                            numOccurrencesPref.edit().putInt(PREF_NUM_OCCURRENCES, 0).apply();
                            numOccurrences = numOccurrencesPref.getInt(PREF_NUM_OCCURRENCES, 0);
                            tvNumOccurrences.setText(String.valueOf(numOccurrences));
                        })
                        .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss()).create().show();
            } else {
                Toast.makeText(getContext(), "Stop first to reset the counter", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        tvKeyword = rootView.findViewById(R.id.tv_keyword);
        tvKeyword.setText(getString(R.string.keyword, keyWord));
        tvKeyword.setOnClickListener(v -> {
            vibrate(30);
            changeKeyword();
        });

        ImageView graphImage = rootView.findViewById(R.id.ic_graph);
        graphImage.setOnClickListener(v -> startActivity(new Intent(getContext(), HistoryActivity.class)));

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(occurrencesReceiver);
        getContext().unregisterReceiver(updateButtonReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Since Android OS might stop the service in the background without cancelling
        // the notification, we need to check if the service is running when the app is
        // re-opened.
        NotificationManager manager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (!isServiceRunning(TwitterService.class) && manager != null) {
            manager.cancel(TwitterService.ID_BOT_CONNECTED);
        }

        // Make sure to update the key word from preferences
        keyWord = keywordPref.getString(PREF_KEYWORD, DEFAULT_KEYWORD);

        // If the key word has been deleted from history, the number
        // of occurrences the TextView should be reset to 0
        if (!utils.doesWordExist(keyWord)) {
            tvNumOccurrences.setText("0");
        }
        updateButtonText();
    }

    @Override
    public void onStop() {
        super.onStop();
        // Since AS gets mad about window leaks, we need to make sure
        // the dialog is cancelled if the device is rotated, or some other
        // event occurs
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(TAG, "onConfigurationChanged: " + newConfig.orientation);
    }

    @SuppressLint("SetTextI18n")
    private void updateButtonText() {
        if (isServiceRunning(TwitterService.class)) {
            startStop.setText("stop");
        } else {
            startStop.setText("start");
        }
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

    private void changeKeyword() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.change_keyword_dialog, getView().findViewById(R.id.dialog_layout));

        builder.setView(view);

        final EditText changeKeyword = view.findViewById(R.id.et_change_keyword);
        builder.setCancelable(false);
        builder.setTitle("Change Keyword")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Even though there's no code here, this is used to make sure
                    // the "OK" button shows up on the dialog
                })
                .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());

        dialog = builder.create();
        dialog.show();
        // This makes sure the dialog only closes if the entered keyword is valid
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            if (!TextUtils.isEmpty(changeKeyword.getText().toString().trim())) {
                String keyWordFromTextView = changeKeyword.getText().toString().trim().toLowerCase();

                // The key word can't be Twitter because the stream thinks
                // every status contains the word 'Twitter'
                if (!keyWordFromTextView.equals(keyWord) && !keyWordFromTextView.equalsIgnoreCase("twitter")) {
                    // Save the keyword from the TextView into a preference.
                    // This way, the same word appears as the keyword when the app opens.
                    keywordPref.edit().putString(PREF_KEYWORD, keyWordFromTextView).apply();
                    tvKeyword.setText(OccurrencesFragment.this.getString(R.string.keyword, keyWordFromTextView));

                    // The keyword exists so we set the counter to the value of the keyword
                    if (utils.doesWordExist(keyWordFromTextView)) {
                        numOccurrencesPref.edit().putInt(PREF_NUM_OCCURRENCES, utils.getHashMap().get(keyWordFromTextView)).apply();
                        numOccurrences = numOccurrencesPref.getInt(PREF_NUM_OCCURRENCES, 0);
                        tvNumOccurrences.setText(String.valueOf(numOccurrences));
                    } else {
                        // The word doesn't exist so we reset the counter
                        numOccurrencesPref.edit().putInt(PREF_NUM_OCCURRENCES, 0).apply();
                        numOccurrences = numOccurrencesPref.getInt(PREF_NUM_OCCURRENCES, 0);
                        tvNumOccurrences.setText(String.valueOf(numOccurrences));
                    }
                    // Make sure to stop the service when the keyword has changed
                    if (isServiceRunning(TwitterService.class)) {
                        getContext().stopService(new Intent(getContext(), TwitterService.class));
                    }
                    dialog.dismiss();
                }
                if (keyWordFromTextView.equalsIgnoreCase("twitter")) {
                    Toast.makeText(getContext(), "Invalid Keyword", Toast.LENGTH_SHORT).show();
                }
            }
            dialog.dismiss();
        });
    }

    private void vibrate(int intensity) {
        Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(intensity);
        }
    }

    /**
     * Receiver to update the number of occurrences and store
     * it in a SharedPreference
     *
     * @see StreamListener#onStatus(Status)
     */
    private BroadcastReceiver occurrencesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int wordCount = intent.getIntExtra("number", 0);
            tvNumOccurrences.setText(String.valueOf(wordCount));
            numOccurrencesPref.edit().putInt(PREF_NUM_OCCURRENCES, wordCount).apply();
        }
    };

    private BroadcastReceiver updateButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive...");
            updateButtonText();
        }
    };
}
