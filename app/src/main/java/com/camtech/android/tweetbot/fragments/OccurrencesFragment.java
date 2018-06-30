package com.camtech.android.tweetbot.fragments;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.activities.HistoryActivity;
import com.camtech.android.tweetbot.activities.SettingsActivity;
import com.camtech.android.tweetbot.core.StreamListener;
import com.camtech.android.tweetbot.services.TimerService;
import com.camtech.android.tweetbot.services.TwitterService;
import com.camtech.android.tweetbot.utils.DbUtils;
import com.camtech.android.tweetbot.utils.ServiceUtils;
import com.camtech.android.tweetbot.utils.TwitterUtils;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import twitter4j.Status;

/**
 * Displays the number of occurrences of a given word.
 * See {@link StreamListener#onStatus(Status)}
 */
public class OccurrencesFragment extends Fragment {

    private final String KEYWORD_KEY = "keyword";
    private final String OCCURRENCE_KEY = "occurrence";
    private static int numOccurrences;
    private static String keyWord;
    private AlertDialog resetKeyWordDialog;
    private static int timeRemaining;
    private Intent timerIntent;

    @BindView(R.id.bt_start_stop) Button startStop;
    @BindView(R.id.tv_keyword) TextView tvKeyword;
    @BindView(R.id.tv_num_occurrences) TextView tvNumOccurrences;
    @BindView(R.id.fragment_occurrence_root) RelativeLayout root;
    @BindView(R.id.iv_graph) ImageView graphImage;
    @BindView(R.id.iv_settings) ImageView settingsImage;
    @BindString(R.string.pref_token) String prefToken;
    @BindString(R.string.pref_token_secret) String prefTokenSecret;
    @BindString(R.string.default_keyword) String defaultKeyword;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_occurrences, container, false);
        ButterKnife.bind(this, rootView);
        // We use a timer in this fragment to make sure the user doesn't
        // make too many auth calls in a short amount of time
        timerIntent = new Intent(requireContext(), TimerService.class);

        if (savedInstanceState == null) {
            initViews();
        } else {
            keyWord = savedInstanceState.getString(KEYWORD_KEY);
            numOccurrences = savedInstanceState.getInt(OCCURRENCE_KEY);
            tvKeyword.setText(getString(R.string.tv_keyword, keyWord));
            tvNumOccurrences.setText(String.valueOf(numOccurrences));
        }
        startStop.setOnClickListener(v -> {
            vibrate();
            // Since this button acts as both a starting and a stopping button,
            // we have to check if the service is running before we start it,
            if (!ServiceUtils.isServiceRunning(requireContext(), TwitterService.class)) {
                if (!TwitterUtils.isUserLoggedIn()) {
                    Snackbar.make(root, "Please login to your Twitter account", Snackbar.LENGTH_LONG)
                            .setAction("LOGIN", view -> startActivity(new Intent(getContext(), SettingsActivity.class)))
                            .show();
                    return;
                }
                // Can't start the service if we're waiting for
                // the timer to finish
                if (!ServiceUtils.isServiceRunning(requireContext(), TimerService.class)) {
                    Intent twitterIntent = new Intent(getContext(), TwitterService.class);
                    // Tell the TwitterService what word we're listening for
                    twitterIntent.putExtra(Intent.EXTRA_TEXT, keyWord);
                    requireContext().startService(twitterIntent);
                    requireContext().startService(timerIntent);
                } else {
                    Toast.makeText(
                            getContext(),
                            getResources().getQuantityString(
                                    R.plurals.time_remaining, timeRemaining, timeRemaining),
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                requireContext().stopService(new Intent(getContext(), TwitterService.class));
                requireContext().stopService(timerIntent);
                requireContext().startService(timerIntent);
            }
        });

        tvKeyword.setOnClickListener(v -> {
            vibrate();
            changeKeyword();
        });

        graphImage.setOnClickListener(v -> {
            vibrate();
            startActivity(new Intent(getContext(), HistoryActivity.class));
        });

        settingsImage.setOnClickListener(v -> {
            vibrate();
            startActivity(new Intent(getContext(), SettingsActivity.class));
        });
        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        requireContext().unregisterReceiver(updateButtonReceiver);
        requireContext().unregisterReceiver(timeRemainingReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireContext().registerReceiver(timeRemainingReceiver, new IntentFilter(TimerService.BROADCAST_TIME));
        // Receiver to update tvNumOccurrences
        requireContext().registerReceiver(occurrencesReceiver, new IntentFilter(StreamListener.OCCURRENCES_INTENT_FILTER));
        // Receiver to make sure the button text updates
        requireContext().registerReceiver(updateButtonReceiver, new IntentFilter(TwitterService.BROADCAST_UPDATE));
        // Since Android OS might stop the service in the background without cancelling
        // the notification, we need to check if the service is running when the app is
        // re-opened.
        NotificationManager manager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (!ServiceUtils.isServiceRunning(requireContext(), TwitterService.class)) {
            if (manager != null) {
                manager.cancel(TwitterService.ID_STREAM_CONNECTED);
            }
        }
        updateButtonText();
        Pair<String, Integer> pair = DbUtils.getKeyWord(requireContext(), keyWord);
        if (pair != null) {
            tvKeyword.setText(getString(R.string.tv_keyword, pair.first));
            tvNumOccurrences.setText(String.valueOf(pair.second));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEYWORD_KEY, keyWord);
        outState.putInt(OCCURRENCE_KEY, numOccurrences);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Since AS gets mad about window leaks, we need to make sure
        // any dialog is cancelled if the device is rotated, or some other
        // event occurs
        if (resetKeyWordDialog != null) resetKeyWordDialog.dismiss();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requireContext().unregisterReceiver(occurrencesReceiver);
    }

    private void updateButtonText() {
        startStop.setText(
                ServiceUtils.isServiceRunning(requireContext(), TwitterService.class)
                        ? R.string.button_stop
                        : R.string.button_start);
    }

    private void initViews() {
        // Load the last keyword from the database into the views
        // or show the default keyword if the database is empty
        Pair<String, Integer> pair = DbUtils.getMostRecentWord(getContext());
        if (pair != null) {
            keyWord = pair.first;
            numOccurrences = pair.second != null ? pair.second : 0;
            tvKeyword.setText(getString(R.string.tv_keyword, pair.first));
            tvNumOccurrences.setText(String.valueOf(pair.second));
        } else {
            keyWord = defaultKeyword;
            tvKeyword.setText(getString(R.string.tv_keyword, keyWord));
            tvNumOccurrences.setText(String.valueOf(0));
        }
    }

    private void changeKeyword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        View view = getLayoutInflater().inflate(
                R.layout.change_keyword_dialog,
                getView().findViewById(R.id.dialog_layout_root));

        EditText etChangeKeyword = view.findViewById(R.id.et_query);
        TextInputLayout textInputLayout = view.findViewById(R.id.text_input_layout);
        textInputLayout.setHint("Keyword");

        builder.setView(view)
                .setCancelable(false)
                .setTitle("Change Keyword")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Even though there's no code here, this is used to make sure
                    // the "OK" button shows up on the dialog
                })
                .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());

        resetKeyWordDialog = builder.create();
        resetKeyWordDialog.show();
        // This makes sure the dialog only closes if the entered keyword is valid
        resetKeyWordDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (!TextUtils.isEmpty(etChangeKeyword.getText().toString().trim())) {
                String keyWordFromTextView = etChangeKeyword.getText().toString().trim().toLowerCase();
                // We need to check if the key word in the dialog's edit text is the same as
                // the keyword that's already set. If it is, just close this dialog. If it's not,
                // we'll then check if the word already exists in the database
                if (!keyWordFromTextView.equals(keyWord)) {
                    Pair<String, Integer> pair = DbUtils.getKeyWord(getContext(), keyWordFromTextView);
                    // The word exists in the database so we'll grab the pair
                    // and set the views
                    if (pair != null) {
                        keyWord = pair.first;
                        numOccurrences = pair.second != null ? pair.second : 0;
                        tvKeyword.setText(getString(R.string.tv_keyword, pair.first));
                        tvNumOccurrences.setText(String.valueOf(pair.second));
                    } else {
                        // The word isn't in the database so we'll
                        // set set the numOccurrences text view to 0
                        keyWord = keyWordFromTextView;
                        numOccurrences = 0;
                        tvKeyword.setText(getString(R.string.tv_keyword, keyWordFromTextView));
                        tvNumOccurrences.setText(String.valueOf(numOccurrences));
                        DbUtils.saveKeyWord(requireContext(), keyWord, numOccurrences);
                    }
                    // Make sure to stop the service when the keyword has changed
                    if (ServiceUtils.isServiceRunning(requireContext(), TwitterService.class)) {
                        requireContext().stopService(new Intent(getContext(), TwitterService.class));
                        requireContext().stopService(timerIntent);
                        requireContext().startService(timerIntent);
                    }
                    resetKeyWordDialog.dismiss();
                }
            }
            resetKeyWordDialog.dismiss();
        });
    }

    private void vibrate() {
        Vibrator v = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(30);
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
            // Get the updated number of occurrences from the stream listener
            int wordCount = intent.getIntExtra(StreamListener.NUM_OCCURRENCES_EXTRA, 0);
            tvNumOccurrences.setText(String.valueOf(wordCount));
        }
    };

    private BroadcastReceiver updateButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateButtonText();
        }
    };

    /**
     * Receiver to get the time since the last query
     *
     * @see TimerService
     */
    private BroadcastReceiver timeRemainingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            timeRemaining = (int) intent.getLongExtra(TimerService.INTENT_TIME_LEFT, 0);
        }
    };
}
