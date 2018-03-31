package com.camtech.android.tweetbot.tweet;

import android.os.Environment;
import android.util.Log;

import com.camtech.android.tweetbot.data.Keys;

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
    private final String TAG = TwitterUtils.class.getSimpleName();
    private HashMap<String, Integer> hashMap;
    private final String FILE_NAME = "Occurrences.dat";
    private final String FOLDER_NAME = "TweetData";

    // Will open to the twitter website
    public static final String BASE_TWITTER_URL = "https://twitter.com/";
    // Will open in the twitter app
    public static final String BASE_TWITTER_URI = "twitter://user?screen_name=";

    public TwitterUtils() {
    }

    public TwitterUtils(Twitter twitter) {
        this.twitter = twitter;
    }

    /**
     * Helper method to authorize the twitter account.
     * This MUST be called before a Twitter object is
     * passed into the constructor.
     */
    public static Twitter setUpTwitter() {
        return new TwitterFactory(getConfig()).getInstance();
    }

    /**
     * Helper method to build the configuration
     */
    public static Configuration getConfig() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(Keys.CONSUMER_KEY)
                .setOAuthConsumerSecret(Keys.CONSUMER_KEY_SECRET)
                .setOAuthAccessToken(Keys.ACCESS_TOKEN)
                .setOAuthAccessTokenSecret(Keys.ACCESS_TOKEN_SECRET);
        return cb.build();
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

    public void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
