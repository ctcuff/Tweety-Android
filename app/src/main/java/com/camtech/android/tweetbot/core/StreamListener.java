package com.camtech.android.tweetbot.core;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.Pair;
import android.support.v7.preference.PreferenceManager;
import android.text.format.DateFormat;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.models.Tweet;
import com.camtech.android.tweetbot.services.TwitterService;
import com.camtech.android.tweetbot.utils.DbUtils;

import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;

/**
 * This is the core part of the app.
 * It handles receiving tweets from Twitter.
 */
public class StreamListener implements StatusListener {

    private int wordCount;
    private Context context;
    private Intent intentUpdateUI;
    private String keyWord;
    private SharedPreferences sharedPreferences;

    public static final String OCCURRENCES_INTENT_FILTER = "occurrences";
    public static final String NEW_TWEET_BROADCAST = "tweet";
    public static final String NUM_OCCURRENCES_EXTRA = "number";
    public static String KEYWORD_BROADCAST_EXTRA = "keyWord";

    public StreamListener(Context context, String keyWord) {
        this.context = context;
        this.keyWord = keyWord;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        // Check to see if the key word is new. If it is,
        // set the number of occurrences to 0. If it's not, get
        // the value from the database.
        Pair<String, Integer> pair = DbUtils.getKeyWord(context, keyWord);
        wordCount = pair != null && pair.second != null ? pair.second : 0;
        // Intent to update the text in Occurrences/Messages fragment
        intentUpdateUI = new Intent(OCCURRENCES_INTENT_FILTER);
    }


    @Override
    public void onStatus(Status status) {
        // This formats the date to appear as: Mon, February 5, 2018 01:27 AM
        String date = DateFormat.format("EEE, MMMM d, yyyy hh:mm aaa", status.getCreatedAt()).toString();
        String screenName = status.getUser().getScreenName();
        String name = status.getUser().getName();
        // Twitter returns truncated retweets so we have to make sure
        //we get the entire text of the tweet
        String message = status.isRetweet()
                ? status.getRetweetedStatus().getText()
                : status.getText();
        String userProfilePic = status.getUser().getBiggerProfileImageURL();
        String userDescription = status.getUser().getDescription();
        long id = status.getId();
        // Check to see if the tweet is a re-tweet
        boolean wasRetweet = status.isRetweet();
        // Check to see if the tweet was in English
        boolean isEnglish = status.getLang().equals("en");
        // Load the boolean values from the checkbox preference in the settings fragment
        boolean canShowRetweets = sharedPreferences.getBoolean(
                context.getString(R.string.pref_show_retweet_streaming_key),
                context.getResources().getBoolean(R.bool.pref_show_retweets_streaming));
        boolean restrictToEnglish = sharedPreferences.getBoolean(
                context.getString(R.string.pref_english_only_key),
                context.getResources().getBoolean(R.bool.pref_english_only_streaming));

        // Package the tweet into an intent so it can be sent via broadcast
        intentUpdateUI.putExtra(
                NEW_TWEET_BROADCAST,
                new Tweet(date, screenName, name, userDescription, userProfilePic, message, keyWord, id));

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

    @Override
    public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {

    }

    @Override
    public void onTrackLimitationNotice(int i) {
    }

    @Override
    public void onScrubGeo(long l, long l1) {

    }

    @Override
    public void onStallWarning(StallWarning stallWarning) {
    }

    @Override
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
            // Error 420 occurs when there are too many auth
            // requests in a short amount of time
            if (error.getMessage().contains("420")) {
                builder.setContentText("Error " + error.getMessage() + "\nPlease wait 60 seconds before trying again");
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
    }

    private void broadcastTweet() {
        wordCount++;
        // Send the word count to the fragments so that the UI updates
        // along with the keyword
        intentUpdateUI.putExtra(NUM_OCCURRENCES_EXTRA, wordCount);
        intentUpdateUI.putExtra(KEYWORD_BROADCAST_EXTRA, keyWord);
        // Send the tweet received to the TweetPostedFragment
        context.sendBroadcast(intentUpdateUI);
    }

}
