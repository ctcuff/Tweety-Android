package com.camtech.android.tweetbot.fragments;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
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
import com.camtech.android.tweetbot.activities.SettingsActivity;
import com.camtech.android.tweetbot.tweet.StreamListener;
import com.camtech.android.tweetbot.tweet.TwitterService;
import com.camtech.android.tweetbot.utils.ServiceUtils;
import com.camtech.android.tweetbot.utils.TwitterUtils;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import twitter4j.Status;

/**
 * Displays the number of occurrences of a given word.
 * See {@link StreamListener#onStatus(Status)}
 */
public class OccurrencesFragment extends Fragment {

    private final String TAG = OccurrencesFragment.class.getSimpleName();
    private int numOccurrences;
    private String keyWord;
    private SharedPreferences keywordPref;
    private SharedPreferences numOccurrencesPref;
    private AlertDialog resetKeyWordDialog;
    private AlertDialog resetOccurrencesDialog;
    private AlertDialog loginDialog;
    public TwitterLoginButton twitterLoginButton;

    @BindView(R.id.bt_start_stop) Button startStop;
    @BindView(R.id.tv_keyword) TextView tvKeyword;
    @BindView(R.id.tv_num_occurrences) TextView tvNumOccurrences;
    @BindString(R.string.pref_token) String prefToken;
    @BindString(R.string.pref_token_secret) String prefTokenSecret;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_occurrences, container, false);
        ButterKnife.bind(this, rootView);

        keywordPref = getContext().getSharedPreferences(getString(R.string.pref_keyword), Context.MODE_PRIVATE);
        numOccurrencesPref = getContext().getSharedPreferences(getString(R.string.pref_num_occurrences), Context.MODE_PRIVATE);
        keyWord = keywordPref.getString(getString(R.string.pref_keyword), getString(R.string.pref_default_keyword));
        numOccurrences = numOccurrencesPref.getInt(getString(R.string.pref_num_occurrences), 0);

        startStop.setOnClickListener(v -> {
            vibrate();
            // Since this button acts as both a starting and a stopping button,
            // we have to check if the service is running before we start it,
            if (!ServiceUtils.isServiceRunning(getContext(), TwitterService.class)) {
                if (!TwitterUtils.isUserLoggedIn()) {
                    showLoginDialog();
                    return;
                }
                getContext().startService(new Intent(getContext(), TwitterService.class));
            } else {
                getContext().stopService(new Intent(getContext(), TwitterService.class));
            }
        });

        tvNumOccurrences.setText(String.valueOf(numOccurrences));
        tvNumOccurrences.setOnClickListener(view -> {
            if (!ServiceUtils.isServiceRunning(getContext(), TwitterService.class)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage("Reset the counter for this word?");
                builder.setPositiveButton("YES", (dialog, which) -> {
                    numOccurrencesPref.edit().putInt(getString(R.string.pref_num_occurrences), 0).apply();
                    numOccurrences = numOccurrencesPref.getInt(getString(R.string.pref_num_occurrences), 0);
                    tvNumOccurrences.setText(String.valueOf(numOccurrences));
                    TwitterUtils.saveHashMap(keyWord, 0);
                });
                builder.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
                resetOccurrencesDialog = builder.create();
                resetOccurrencesDialog.show();
            } else {
                Toast.makeText(getContext(), "Stop first to reset the counter", Toast.LENGTH_SHORT).show();
            }
        });
        tvKeyword.setText(getString(R.string.tv_keyword, keyWord));
        tvKeyword.setOnClickListener(v -> {
            vibrate();
            changeKeyword();
        });

        ImageView graphImage = rootView.findViewById(R.id.iv_graph);
        graphImage.setOnClickListener(v -> startActivity(new Intent(getContext(), HistoryActivity.class)));

        ImageView settingsImage = rootView.findViewById(R.id.iv_settings);
        settingsImage.setOnClickListener(v -> startActivity(new Intent(getContext(), SettingsActivity.class)));

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        getContext().unregisterReceiver(occurrencesReceiver);
        getContext().unregisterReceiver(updateButtonReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Receiver to update tvNumOccurrences
        getContext().registerReceiver(occurrencesReceiver, new IntentFilter(StreamListener.LISTENER_BROADCAST));
        // Receiver to make sure the button text updates
        getContext().registerReceiver(updateButtonReceiver, new IntentFilter(TwitterService.BROADCAST_UPDATE));

        // Since Android OS might stop the service in the background without cancelling
        // the notification, we need to check if the service is running when the app is
        // re-opened.
        NotificationManager manager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (!ServiceUtils.isServiceRunning(getContext(), TwitterService.class) && manager != null) {
            manager.cancel(TwitterService.ID_BOT_CONNECTED);
        }

        // Make sure to update the key word from preferences
        keyWord = keywordPref.getString(getString(R.string.pref_keyword), getString(R.string.pref_default_keyword));

        // If the key word has been deleted from history, the number
        // of occurrences the TextView should be reset to 0
        if (!TwitterUtils.doesWordExist(keyWord)) {
            tvNumOccurrences.setText("0");
        } else {
            // A card from the history activity was clicked
            // so we need to update the text views to that clicked word
            numOccurrences = numOccurrencesPref.getInt(getString(R.string.pref_num_occurrences), 0);
            tvNumOccurrences.setText(String.valueOf(numOccurrences));
            tvKeyword.setText(getString(R.string.tv_keyword, keyWord));
        }
        updateButtonText();
        checkOrientation();
    }

    @Override
    public void onStop() {
        super.onStop();
        // Since AS gets mad about window leaks, we need to make sure
        // any dialog is cancelled if the device is rotated, or some other
        // event occurs
        if (resetKeyWordDialog != null) resetKeyWordDialog.dismiss();
        if (resetOccurrencesDialog != null) resetOccurrencesDialog.dismiss();
        if (loginDialog != null) loginDialog.dismiss();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        twitterLoginButton.onActivityResult(requestCode, resultCode, data);
        if (loginDialog != null) loginDialog.dismiss();
    }

    private void updateButtonText() {
        if (ServiceUtils.isServiceRunning(getContext(), TwitterService.class)) {
            startStop.setText(R.string.button_stop);
        } else {
            startStop.setText(R.string.button_start);
        }
    }

    private void changeKeyword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        View view = getLayoutInflater().inflate(
                R.layout.change_keyword_dialog,
                getView().findViewById(R.id.dialog_layout_root));

        EditText changeKeyword = view.findViewById(R.id.et_query);
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

            if (!TextUtils.isEmpty(changeKeyword.getText().toString().trim())) {
                String keyWordFromTextView = changeKeyword.getText().toString().trim().toLowerCase();

                // The key word can't be Twitter because the stream thinks
                // every status contains the word 'Twitter'
                if (!keyWordFromTextView.equals(keyWord) && !keyWordFromTextView.equalsIgnoreCase("twitter")) {
                    // Save the keyword from the TextView into a preference.
                    // This way, the same word appears as the keyword when the app opens.
                    keywordPref.edit().putString(getString(R.string.pref_keyword), keyWordFromTextView).apply();
                    tvKeyword.setText(getString(R.string.tv_keyword, keyWordFromTextView));

                    // The keyword exists so we set the counter to the value of the keyword
                    if (TwitterUtils.doesWordExist(keyWordFromTextView)) {
                        numOccurrencesPref.edit().putInt(
                                getString(R.string.pref_num_occurrences), TwitterUtils.getHashMap().get(keyWordFromTextView)).apply();
                        numOccurrences = numOccurrencesPref.getInt(getString(R.string.pref_num_occurrences), 0);
                        tvNumOccurrences.setText(String.valueOf(numOccurrences));
                    } else {
                        // The word doesn't exist so we reset the counter
                        numOccurrencesPref.edit().putInt(getString(R.string.pref_num_occurrences), 0).apply();
                        numOccurrences = numOccurrencesPref.getInt(getString(R.string.pref_num_occurrences), 0);
                        tvNumOccurrences.setText(String.valueOf(numOccurrences));
                    }
                    // Make sure to stop the service when the keyword has changed
                    if (ServiceUtils.isServiceRunning(getContext(), TwitterService.class)) {
                        getContext().stopService(new Intent(getContext(), TwitterService.class));
                    }
                    resetKeyWordDialog.dismiss();
                }
                if (keyWordFromTextView.equalsIgnoreCase("twitter")) {
                    Toast.makeText(getContext(), "Invalid Keyword", Toast.LENGTH_SHORT).show();
                }
            }
            resetKeyWordDialog.dismiss();
        });
    }

    private void vibrate() {
        Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
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
            int wordCount = intent.getIntExtra(StreamListener.NUM_OCCURRENCES_BROADCAST, 0);
            tvNumOccurrences.setText(String.valueOf(wordCount));
            numOccurrencesPref.edit().putInt(getString(R.string.pref_num_occurrences), wordCount).apply();
        }
    };

    private BroadcastReceiver updateButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateButtonText();
        }
    };

    private void checkOrientation() {
        // We need to change the width of the keyword text view so that it can
        // show longer strings when rotated horizontally
        ViewGroup.LayoutParams params = tvKeyword.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            params.width = getResources().getDimensionPixelSize(R.dimen.tv_keyword_portrait);
        } else {
            params.width = getResources().getDimensionPixelSize(R.dimen.tv_keyword_landscape);
        }
    }

    private void showLoginDialog() {
        View v = getLayoutInflater().inflate(
                R.layout.dialog_login,
                getView().findViewById(R.id.dialog_layout_root));
        twitterLoginButton = v.findViewById(R.id.twitter_login);
        twitterLoginButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                TwitterUtils.handleTwitterSession(result.data, getActivity());
            }

            @Override
            public void failure(TwitterException exception) {
                Log.i(TAG, "failure: " + exception.getMessage());
            }
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(v);
        builder.setNegativeButton("CLOSE", (dialog, which) -> dialog.dismiss());
        // Gives the title a blue color, provided the device is running API 24+
        builder.setTitle(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? Html.fromHtml("<font color='#1DA1F2'>Login</font>", Html.FROM_HTML_MODE_LEGACY)
                : "Login");

        loginDialog = builder.create();
        loginDialog.show();
    }
}
