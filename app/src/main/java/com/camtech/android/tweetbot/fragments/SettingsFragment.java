package com.camtech.android.tweetbot.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.activities.SettingsActivity;
import com.camtech.android.tweetbot.utils.TwitterUtils;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

public class SettingsFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    private final String TAG = SettingsFragment.class.getSimpleName();
    private SettingsActivity settingsActivity;
    private TwitterLoginButton twitterLoginButton;
    private AlertDialog loginDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Get a reference to the SettingsActivity once this fragment
        // has been attached to a Context object
        settingsActivity = (SettingsActivity) context;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_tweets);
        findPreference(getString(R.string.pref_logout_key)).setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_sign_in_key)).setOnPreferenceClickListener(this);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals(getString(R.string.pref_logout_key))) {
            TwitterUtils.logout(getContext());
            if (settingsActivity.getSupportActionBar() != null) {
                settingsActivity.getSupportActionBar().setTitle("");
            }
        } else if (preference.getKey().equals(getString(R.string.pref_sign_in_key))) {
            showLoginDialog();
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Let Twitter handle the auth process
        twitterLoginButton.onActivityResult(requestCode, resultCode, data);
        if (loginDialog != null && loginDialog.isShowing()) loginDialog.dismiss();
    }

    @Override
    public void onStop() {
        super.onStop();
        // Gotta handle them dialog leaks
        if (loginDialog != null) loginDialog.dismiss();
    }

    private void showLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(settingsActivity);
        View view = getLayoutInflater().inflate(R.layout.dialog_login, getView().findViewById(R.id.dialog_layout_root));
        twitterLoginButton = view.findViewById(R.id.twitter_login);
        twitterLoginButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                TwitterUtils.handleTwitterSession(result.data, settingsActivity);
            }

            @Override
            public void failure(TwitterException exception) {
                Log.i(TAG, "failure: " + exception.getMessage());
                Toast.makeText(getContext(), "Error logging in", Toast.LENGTH_LONG).show();
            }
        });
        builder.setView(view);
        builder.setTitle(
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                        ? Html.fromHtml("<font color='#1DA1F2'>Login</font>", Html.FROM_HTML_MODE_LEGACY)
                        : "Login");
        builder.setNegativeButton("CLOSE", (dialog, which) -> dialog.dismiss());
        loginDialog = builder.create();
        loginDialog.show();
    }
}
