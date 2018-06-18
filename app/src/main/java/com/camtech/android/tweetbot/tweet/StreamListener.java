package com.camtech.android.tweetbot.tweet;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v7.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.data.Tweet;
import com.camtech.android.tweetbot.utils.TwitterUtils;

import java.util.HashMap;

import twitter4j.DirectMessage;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.UserStreamListener;

/**
 * This is the core part of the app. It handles receiving tweets/messages
 * from Twitter.
 */
public class StreamListener implements UserStreamListener {

    private static final String TAG = StreamListener.class.getSimpleName();

    private TwitterUtils utils;
    private int wordCount;
    private Context context;
    private Intent intentUpdateUI;
    private String keyWord;
    private SharedPreferences sharedPreferences;

    public static final String LISTENER_BROADCAST = "occurrences";
    public static final String NEW_TWEET_BROADCAST = "tweet";
    public static final String NUM_OCCURRENCES_BROADCAST = "number";

    /**
     * Constructor used to show the occurrences of a given word.
     * Mainly used in {@link #onStatus(Status)}
     */
    StreamListener(Context context, String keyWord) {
        Log.i(TAG, "Listening for occurrences of " + keyWord);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.context = context;
        this.keyWord = keyWord;

        utils = new TwitterUtils();

        // Check to see if the key word is new. If it is,
        // set the number of occurrences to 0. If it's not, get
        // the value from the saved HashMap.
        HashMap<String, Integer> hashMap = utils.getHashMap();
        if (hashMap != null && utils.doesWordExist(keyWord)) {
            wordCount = hashMap.get(keyWord);
        } else {
            wordCount = 0;
        }
        // Intent to update the text in Occurrences/Messages fragment
        intentUpdateUI = new Intent(LISTENER_BROADCAST);
    }

    public void onDeletionNotice(long l, long l1) {

    }

    public void onFriendList(long[] longs) {
    }

    public void onFavorite(User user, User user1, Status status) {

    }

    public void onUnfavorite(User user, User user1, Status status) {

    }

    public void onFollow(User source, User followedUser) {
    }

    public void onUnfollow(User user, User user1) {

    }

    public void onDirectMessage(DirectMessage directMessage) {
    }

    public void onUserListMemberAddition(User user, User user1, UserList userList) {

    }

    public void onUserListMemberDeletion(User user, User user1, UserList userList) {

    }

    public void onUserListSubscription(User user, User user1, UserList userList) {
    }

    public void onUserListUnsubscription(User user, User user1, UserList userList) {

    }

    public void onUserListCreation(User user, UserList userList) {
    }

    public void onUserListUpdate(User user, UserList userList) {
    }

    public void onUserListDeletion(User user, UserList userList) {

    }

    public void onUserProfileUpdate(User user) {

    }

    public void onUserSuspension(long l) {

    }

    public void onUserDeletion(long l) {

    }

    public void onBlock(User user, User user1) {

    }

    public void onUnblock(User user, User user1) {

    }

    public void onRetweetedRetweet(User user, User user1, Status status) {

    }

    public void onFavoritedRetweet(User user, User user1, Status status) {
    }

    public void onQuotedTweet(User user, User user1, Status status) {

    }

    public void onStatus(Status status) {

        // This formats the date to appear as: Mon, February 5, 2018 01:27 AM
        String date = DateFormat.format("EEE, MMMM d, yyyy hh:mm aaa", status.getCreatedAt()).toString();
        String screenName = status.getUser().getScreenName();
        String name = status.getUser().getName();
        String message = status.getText();
        String userProfilePic = status.getUser().getBiggerProfileImageURL();
        String userDescription = status.getUser().getDescription();

        // Check to see if the tweet is a re-tweet
        boolean wasRetweet = message.startsWith("RT");
        // Check to see if the tweet was in English
        boolean isEnglish = status.getLang().equals("en");
        // Load the boolean values from the checkbox preference in the settings fragment
        boolean canShowRetweets = sharedPreferences.getBoolean(context.getString(R.string.pref_show_retweets_key),
                context.getResources().getBoolean(R.bool.pref_show_retweets));
        boolean restrictToEnglish = sharedPreferences.getBoolean(context.getString(R.string.pref_english_only_key),
                context.getResources().getBoolean(R.bool.pref_english_only));

        // Package the tweet into an intent so it can be sent via broadcast
        intentUpdateUI.putExtra(NEW_TWEET_BROADCAST,
                new Tweet(date, screenName, name, userDescription, userProfilePic, message, keyWord));

        if (canShowRetweets && restrictToEnglish) {
            if (isEnglish) broadcastTweet();
        }

        if (!canShowRetweets && restrictToEnglish) {
            if (!wasRetweet && isEnglish) broadcastTweet();
        }

        if (!canShowRetweets && !restrictToEnglish) {
            if (!wasRetweet) broadcastTweet();
        }

        if (canShowRetweets && !restrictToEnglish) {
           broadcastTweet();
        }

    }

    private void broadcastTweet() {
        wordCount++;
        // Send the word count to the fragments so that the UI updates
        intentUpdateUI.putExtra(NUM_OCCURRENCES_BROADCAST, wordCount);
        // Send the tweet received to the TweetPostedFragment
        context.sendBroadcast(intentUpdateUI);
    }

    public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {

    }

    public void onTrackLimitationNotice(int i) {
    }

    public void onScrubGeo(long l, long l1) {

    }

    public void onStallWarning(StallWarning stallWarning) {
        Log.i(TAG, "STALLING, " + stallWarning.getMessage());
    }

    public void onException(Exception error) {
        context.stopService(new Intent(context, TwitterService.class));

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "Exception");

        builder.setStyle(new NotificationCompat.BigTextStyle());
        builder.setSmallIcon(R.drawable.ic_stat_message);
        builder.setContentTitle(context.getString(R.string.notification_title));
        builder.setColor(context.getResources().getColor(R.color.colorNotificationError));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setPriority(NotificationManager.IMPORTANCE_HIGH);
        }
        // If there's no mobile data, we want to only show
        // the notification in the TwitterService
        if (!error.getMessage().contains("Unable to resolve host")) {
            // Error 402 occurs when there are too many auth
            // requests in a short amount of time
            if (error.getMessage().contains("420")) {
                builder.setContentText("Error " + error.getMessage() + "\nPlease wait 30 seconds before trying again");
            } else {
                builder.setContentText("Error " + error.getMessage());
            }
            // Don't vibrate if the user's device is on silent
            if (audio != null) {
                switch (audio.getRingerMode()) {
                    case AudioManager.RINGER_MODE_NORMAL:
                    case AudioManager.RINGER_MODE_VIBRATE:
                        builder.setVibrate(new long[]{0, 250, 250, 250});
                        break;
                    case AudioManager.RINGER_MODE_SILENT:
                        builder.setVibrate(new long[]{0});
                        break;
                }
            }
            if (manager != null) {
                manager.notify(2, builder.build());
            }
        }
        Log.i(TAG, "onException: ", error);
    }
}
