package com.camtech.android.tweetbot.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.data.Keys;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.TwitterAuthProvider;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterSession;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import twitter4j.DirectMessage;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Contains various helper methods.
 */
public class TwitterUtils {
    private Twitter twitter;
    private static final int DELAY = 1000;
    private static final String TAG = TwitterUtils.class.getSimpleName();
    private HashMap<String, Integer> hashMap;
    private final String FILE_NAME = "Occurrences.dat";
    private final String FOLDER_NAME = "TweetData";

    // Will open to the twitter website
    public static final String BASE_TWITTER_URL = "https://twitter.com/";
    // Will open the user's profile in the twitter app
    public static final String BASE_TWITTER_URI = "twitter://user?screen_name=";
    // Will open a specific tweet in the Twitter app
    public static final String BASE_TWITTER_STATUS_URI = "twitter://status?status_id=";

    public TwitterUtils() {
    }

    public TwitterUtils(Twitter twitter) {
        this.twitter = twitter;
    }

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
                .setOAuthConsumerKey(Keys.CONSUMER_KEY)
                .setOAuthConsumerSecret(Keys.CONSUMER_KEY_SECRET)
                .setOAuthAccessToken(token)
                .setOAuthAccessTokenSecret(tokenSecret);
        return new TwitterFactory(cb.build()).getInstance();
    }

    /**
     * Sends a message to the specified user using the users screen name
     */
    public void sendMessage(String user, String message) {
        try {
            DirectMessage directMessage = twitter.sendDirectMessage(user, message);
            Log.v(TAG, "-----------------------------------");
            Log.v(TAG, "Sent:\n" + message + "\nTo @" + directMessage.getRecipientScreenName());
            Log.v(TAG, "-----------------------------------");
            sleep(DELAY);
        } catch (TwitterException e) {
            Log.v(TAG, "Error sending message" + e);
        }
    }

    /**
     * Utility function to save the keyword and its number of
     * occurrences into a HashMap. Note that only one file is ever
     * created, so every time data is saved, the data is appended
     * to the file.
     *
     * @param keyWord        The word to be saved to the hash map. This value
     *                       should always be paired with it's number of occurrences
     * @param numOccurrences The number of occurrences for the given word. The should
     *                       again match with the keyword
     */
    public void saveHashMap(String keyWord, int numOccurrences) {
        String path = Environment.getExternalStorageDirectory().toString() + "/" + FOLDER_NAME;
        File folder = new File(path);
        if (!folder.mkdir()) {
            Log.i(TAG, "saveState: folder already exists");
        } else {
            Log.i(TAG, "saveState: folder created");
        }

        File file = new File(folder, FILE_NAME);
        if (file.exists()) {
            // If the HashMap already exists, retrieve it from storage
            // and add the keyword/number of occurrences
            hashMap = getHashMap();
            hashMap.put(keyWord, numOccurrences);
        } else {
            // The map doesn't exist so we make a new one
            hashMap = new HashMap<>();
            hashMap.put(keyWord, numOccurrences);
        }
        try {
            // Write the HashMap to Occurrences.dat
            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
            outputStream.writeObject(hashMap);
            outputStream.flush();
            outputStream.close();
            Log.i(TAG, "saveState: File saved to " + file.getPath());
        } catch (IOException e) {
            Log.i(TAG, "saveState... error saving", e);
        }
    }

    /**
     * Used to save an entire HashMap instead of
     * a single key value pair
     */
    public void saveHashMap(HashMap<String, Integer> map) {
        String path = Environment.getExternalStorageDirectory().toString() + "/" + FOLDER_NAME;
        File folder = new File(path);
        if (!folder.mkdir()) {
            Log.i(TAG, "saveState: folder already exists");
        } else {
            Log.i(TAG, "saveState: folder created");
        }
        File file = new File(folder, FILE_NAME);
        if (file.exists()) {
            try {
                // Write the HashMap to Occurrences.ser
                ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
                outputStream.writeObject(map);
                outputStream.flush();
                outputStream.close();
                Log.i(TAG, "saveState: File saved to " + file.getPath());
            } catch (IOException e) {
                Log.i(TAG, "saveState... error saving", e);
            }
        } else {
            Log.i(TAG, "saveHashMap: FILE NOT SAVED");
        }
    }

    /**
     * Returns the HashMap from storage if it exists.
     */
    public HashMap<String, Integer> getHashMap() {
        // This is where the map is saved
        String path = Environment.getExternalStorageDirectory().toString() + "/" + FOLDER_NAME + "/" + FILE_NAME;
        try {
            // Get the saved HashMap if it exists
            FileInputStream fileInputStream = new FileInputStream(path);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            return (HashMap<String, Integer>) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Check to see if the given word exists
     * within the HashMap
     */
    public boolean doesWordExist(String keyWord) {
        String path = Environment.getExternalStorageDirectory().toString() + "/" + FOLDER_NAME + "/" + FILE_NAME;
        try {
            // Check to see if the word exists in the HashMap file
            FileInputStream fileInputStream = new FileInputStream(path);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            hashMap = (HashMap<String, Integer>) objectInputStream.readObject();
            for (Map.Entry<String, Integer> map : hashMap.entrySet()) {
                if (keyWord.equals(map.getKey())) {
                    Log.i(TAG, "WORD EXISTS");
                    return true;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            Log.i(TAG, "Error checking keyword", e);
        }
        return false;
    }

    /**
     * Handles authentication with Twitter. Once the process has completed
     * successfully, the access token and access token secret are saved to a shared preference
     * so various Twitter methods can be called
     */
    public static void handleTwitterSession(TwitterSession session, Activity activity) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        SharedPreferences credentialsPref = activity.getSharedPreferences(
                activity.getString(R.string.pref_auth),
                Context.MODE_PRIVATE);

        AuthCredential credential = TwitterAuthProvider.getCredential(
                session.getAuthToken().token,
                session.getAuthToken().secret);

        Log.i(TAG, "handleTwitterSession: TOKEN " +  session.getAuthToken().token);
        Log.i(TAG, "handleTwitterSession: TOKEN SECRET " +  session.getAuthToken().secret);

        auth.signInWithCredential(credential).addOnCompleteListener(activity, task -> {
            if (task.isSuccessful()) {
                Log.i(TAG, "Auth: success");
                // Store the access token and token secret in a shared preference so
                // we can access different Twitter methods later
                credentialsPref.edit()
                        .putString(activity.getString(R.string.pref_token), session.getAuthToken().token)
                        .putString(activity.getString(R.string.pref_token_secret), session.getAuthToken().secret)
                        .apply();
            } else {
                Log.i(TAG, "Auth: failure ", task.getException());
                Toast.makeText(activity, "Authentication failed.", Toast.LENGTH_SHORT).show();
            }
        });
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
    public static void logout() {
        if (isUserLoggedIn()) {
            FirebaseAuth.getInstance().signOut();
            TwitterCore.getInstance().getSessionManager().clearActiveSession();
            // TODO clear out the shared preferences
        } else {
            Log.i(TAG, "User is already logged out!");
        }
    }

    /**
     * Creates a URL for a specific status that opens to the Twitter website
     *
     * @param screenName The user of the status
     * @param statusId   The ID of the status
     */
    public static String getTwitterStatusUrl(String screenName, long statusId) {
        return String.format("https://twitter.com/%s/status/%s", screenName, String.valueOf(statusId));
    }

    public void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
