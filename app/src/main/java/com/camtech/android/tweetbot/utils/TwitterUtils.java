package com.camtech.android.tweetbot.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.models.Keys;
import com.google.firebase.auth.FirebaseAuth;
import com.twitter.sdk.android.core.TwitterCore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Contains various helper methods.
 */
public class TwitterUtils {
    private static final String TAG = TwitterUtils.class.getSimpleName();
    /**
     * Will open to the twitter website
     */
    private static final String BASE_TWITTER_URL = "https://twitter.com/";
    /**
     * Will open the user's profile in the twitter app
     */
    private static final String BASE_TWITTER_URI = "twitter://user?screen_name=";
    /**
     * Will open a specific tweet in the Twitter app
     */
    private static final String BASE_TWITTER_STATUS_URI = "twitter://status?status_id=";

    /**
     * Helper method to build the configuration
     */
    public static Configuration getConfig(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.pref_auth),
                Context.MODE_PRIVATE);
        String token = sharedPreferences.getString(context.getString(R.string.pref_token), null);
        String tokenSecret = sharedPreferences.getString(context.getString(R.string.pref_token_secret), null);

        if (token == null || tokenSecret == null) {
            Log.i(TAG, "getConfig: Tokens were null");
            return null;
        }

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(Keys.CONSUMER_KEY)
                .setOAuthConsumerSecret(Keys.CONSUMER_KEY_SECRET)
                .setOAuthAccessToken(token)
                .setOAuthAccessTokenSecret(tokenSecret);
        return cb.build();
    }

    /**
     * Uses the token and token secret stored in shared preferences
     * to authenticate a user
     *
     * @return a {@link Twitter} object if the user is authenticated, null otherwise
     */
    public static Twitter getTwitter(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.pref_auth),
                Context.MODE_PRIVATE);
        String token = sharedPreferences.getString(context.getString(R.string.pref_token), null);
        String tokenSecret = sharedPreferences.getString(context.getString(R.string.pref_token_secret), null);

        if (token == null || tokenSecret == null) {
            Log.i(TAG, "getTwitter: Tokens were null");
            return null;
        }

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setTweetModeExtended(true)
                .setOAuthConsumerKey(Keys.CONSUMER_KEY)
                .setOAuthConsumerSecret(Keys.CONSUMER_KEY_SECRET)
                .setOAuthAccessToken(token)
                .setOAuthAccessTokenSecret(tokenSecret);
        return new TwitterFactory(cb.build()).getInstance();
    }

    /**
     * Checks if the user is currently logged in to Twitter.
     *
     * @return true if the user is logged in
     */
    public static boolean isUserLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    /**
     * Logs a user out of Twitter (only if they aren't already logged out)
     * and clears the access token and access token secret from shared preferences.
     */
    public static void logout(Context context) {
        if (isUserLoggedIn()) {
            FirebaseAuth.getInstance().signOut();
            TwitterCore.getInstance().getSessionManager().clearActiveSession();
            // Gotta make sure to clear out the access tokens
            // from shared preferences
            SharedPreferences credentialsPref = context.getSharedPreferences(
                    context.getString(R.string.pref_auth),
                    Context.MODE_PRIVATE);
            credentialsPref.edit().clear().apply();
            Toast.makeText(context, "Successfully logged out", Toast.LENGTH_LONG).show();
        } else {
            Log.i(TAG, "User is already logged out!");
            Toast.makeText(context, "You're already logged out", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Gets rid of any URLs from a string. (Sometimes Twitter tweets/statuses have links)
     * that are cut off making them un-clickable so it's best to just remove them)
     */
    public static String stripUrlFromMessage(String message) {
        try {
            String urlPattern = "((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?+-=\\\\.&])";
            Pattern pattern = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(message);
            int i = 0;
            while (matcher.find()) {
                message = message.replaceAll(matcher.group(i), "").trim();
                i++;
            }
        } catch (PatternSyntaxException e) {
            e.printStackTrace();
        }
        return message;
    }

    /**
     * Opens the profile of a user in the Twitter app if the user has Twitter
     * installed or in a browser if the user doesn't have Twitter installed
     */
    public static void openUserProfile(Context context, String screenName) {
        try {
            // Opens the user in the Twitter app
            context.startActivity(
                    new Intent(Intent.ACTION_VIEW,
                            Uri.parse(TwitterUtils.BASE_TWITTER_URI + screenName)));
        } catch (Exception e) {
            // The user doesn't have Twitter installed
            //  so open to the Twitter website
            context.startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(TwitterUtils.BASE_TWITTER_URL + screenName)));
        }
    }

    /**
     * Opens a status in the Twitter app if the user has Twitter installed or
     * in a browser if the user doesn't have Twitter installed
     */
    public static void openStatus(Context context, String screenName, long statusId) {
        try {
            // Opens the status in the Twitter app
            context.startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(TwitterUtils.BASE_TWITTER_STATUS_URI + statusId)));
        } catch (Exception e) {
            // The user doesn't have Twitter installed
            // so open to the Twitter website
            context.startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(TwitterUtils.createTwitterStatusUrlForWeb(screenName, statusId))));
        }
    }

    /**
     * Creates a URL for a specific status that opens to the Twitter website
     *
     * @param screenName The user of the status
     * @param statusId   The ID of the status
     */
    private static String createTwitterStatusUrlForWeb(String screenName, long statusId) {
        return String.format("https://twitter.com/%s/status/%s", screenName, String.valueOf(statusId));
    }
}
