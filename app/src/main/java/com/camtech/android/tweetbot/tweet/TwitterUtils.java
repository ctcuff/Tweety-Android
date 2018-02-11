package com.camtech.android.tweetbot.tweet;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import twitter4j.DirectMessage;
import twitter4j.Status;
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
    public static TwitterFactory setUpBot() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(Keys.CONSUMER_KEY)
                .setOAuthConsumerSecret(Keys.CONSUMER_KEY_SECRET)
                .setOAuthAccessToken(Keys.ACCESS_TOKEN)
                .setOAuthAccessTokenSecret(Keys.ACCESS_TOKEN_SECRET);
        return new TwitterFactory(cb.build());
    }

    /**
     * Helper method to build the configuration
     */
    public static Configuration setUpConfig() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(Keys.CONSUMER_KEY)
                .setOAuthConsumerSecret(Keys.CONSUMER_KEY_SECRET)
                .setOAuthAccessToken(Keys.ACCESS_TOKEN)
                .setOAuthAccessTokenSecret(Keys.ACCESS_TOKEN_SECRET);
        return cb.build();
    }

    public void updateStatus(String message) {
        Status status = null;
        try {
            status = twitter.updateStatus(message);
        } catch (TwitterException e) {
            e.printStackTrace();
        }

        if (status != null) {
            Log.v(TAG, "Successfully updated the status to:\n" + status.getText());
        }
    }

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

    public void sendMessage(long user, String message) {
        try {
            DirectMessage directMessage = twitter.sendDirectMessage(user, message);
            Log.v(TAG, "-----------------------------------");
            Log.v(TAG, "Sent:\n" + message + "\nTo @" + directMessage.getRecipientScreenName());
            Log.v(TAG, "-----------------------------------");
        } catch (TwitterException e) {
            Log.v(TAG, "Error sending message\n" + e.getMessage());
        }
    }

    public void getTimeline() {
        List<Status> statuses = null;
        try {
            statuses = twitter.getHomeTimeline();
        } catch (TwitterException e) {
            e.printStackTrace();
        }
        Log.v(TAG, "Showing home timeline.");
        if (statuses != null) {
            for (Status status : statuses) {
                Log.v(TAG, status.getUser().getName() + ": " + status.getText());
            }
        }
    }

    public void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Utility function to save the keyword and its number of
     * occurrences into a HashMap. Note that only one file is ever
     * created, so every time data is saved, the data is appended
     * to the file.
     * */
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
            // Write the HashMap to Occurrences.ser
            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
            outputStream.writeObject(hashMap);
            outputStream.flush();
            outputStream.close();
            Log.i(TAG, "saveState: File saved to " + file.getPath());
            Log.i(TAG, "saveHashMap: " + hashMap.toString());
        } catch (IOException e) {
            Log.i(TAG, "saveState... error saving", e);
        }
    }

    /**
     * Used to save an entire HashMap instead of
     * a single key value pair
     * */
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
                Log.i(TAG, "saveHashMap: " + map.toString());
            } catch (IOException e) {
                Log.i(TAG, "saveState... error saving", e);
            }
        } else {
            Log.i(TAG, "saveHashMap: FILE NOT SAVED");
        }

    }

    /**
     * Returns the HashMap from storage if it exists.
     * */
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
     * */
    public boolean doesWordExist(String keyWord) {
        String path = Environment.getExternalStorageDirectory().toString() + "/" + FOLDER_NAME + "/" + FILE_NAME;
        try {
            // Check to see if the word exists in the HashMap file
            FileInputStream fileInputStream = new FileInputStream(path);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            hashMap = (HashMap<String, Integer>) objectInputStream.readObject();
            for (Map.Entry<String, Integer> map : hashMap.entrySet()) {
                if (keyWord.equals(map.getKey())) {
                    Log.i(TAG, "onClick: WORD EXISTS");
                    return true;
                }
            }
        } catch (IOException | ClassNotFoundException ignored) {
        }
        return false;
    }

}
