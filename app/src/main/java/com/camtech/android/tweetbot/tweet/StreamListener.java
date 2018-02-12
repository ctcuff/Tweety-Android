package com.camtech.android.tweetbot.tweet;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.camtech.android.tweetbot.R;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import twitter4j.DirectMessage;
import twitter4j.Relationship;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.UserStreamListener;

import static com.camtech.android.tweetbot.tweet.Dictionary.botGreetings;
import static com.camtech.android.tweetbot.tweet.Dictionary.misunderstandings;
import static com.camtech.android.tweetbot.tweet.Dictionary.quotes;
import static com.camtech.android.tweetbot.tweet.Dictionary.userGreetings;

public class StreamListener implements UserStreamListener {

    private static final String TAG = StreamListener.class.getSimpleName();

    private Twitter twitter;
    private TwitterUtils utils;
    private int wordCount;
    private Context context;
    // The number of strings in the arrays
    private final int NUM_QUOTES = quotes.length;
    private static final int NUM_GREETING = Dictionary.botGreetings.length;
    private static final int NUM_MISUNDERSTANDINGS = misunderstandings.length;
    public static final String OCCURRENCES_BROADCAST = "occurrences";
    private Intent intentUpdateUI;
    private String keyWord;
    private String botScreenName;

    /**
     * Public constructor used to show the occurrences of a given word.
     * Mainly used in {@link #onStatus(Status)}
     */
    StreamListener(Context context, Twitter twitter, String keyWord) {
        Log.i(TAG, "Listening for occurrences of " + keyWord);
        this.context = context;
        this.twitter = twitter;

        // Intent to update the text in Occurrences/Messages fragment
        intentUpdateUI = new Intent(OCCURRENCES_BROADCAST);

        utils = new TwitterUtils(twitter);

        HashMap<String, Integer> hashMap = utils.getHashMap();
        if (hashMap != null && utils.doesWordExist(keyWord)) {
            wordCount = hashMap.get(keyWord);
        } else {
            wordCount = 0;
            this.keyWord = keyWord;
        }

    }

    StreamListener(Context context, Twitter twitter) {
        new ConnectionUtils().execute();
        this.context = context;
        this.twitter = twitter;
        utils = new TwitterUtils(twitter);
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
        try {
            // Automatically follow a new user, if the bot isn't already following them
            Relationship relationship = twitter.showFriendship(followedUser.getScreenName(), source.getScreenName());
            if (relationship.isSourceFollowingTarget()) {
                Log.i(TAG, "Source is followed by target");
            } else {
                Log.i(TAG, "Source is not followed by target, now following @" + source.getScreenName());
                twitter.createFriendship(source.getScreenName());
            }
        } catch (TwitterException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "-----------------------------------");
        Log.i(TAG, "onFollow...");
        Log.i(TAG, "@" + source.getScreenName() + " followed you");
        utils.sendMessage(source.getScreenName(), "Thanks for following, here's a quote:");
        utils.sleep(2000);
        utils.sendMessage(source.getScreenName(), quotes[new Random().nextInt(NUM_QUOTES)]);
        Log.i(TAG, "-----------------------------------");
    }

    public void onUnfollow(User user, User user1) {

    }

    public void onDirectMessage(DirectMessage directMessage) {
        List<String> list = Arrays.asList(userGreetings);
        String message = directMessage.getText().toLowerCase().trim();
        boolean sentByBot = directMessage.getSenderScreenName().equalsIgnoreCase(botScreenName);
        boolean wasGreeting = list.stream().anyMatch(message::contains);
        boolean wantsQuote = message.contains("quote");
        boolean wantsJoke = message.contains("joke");
        boolean wantsRandomWiki = message.contains("random article") || message.contains("random");
        boolean wasGoogleSearch = message.startsWith("search google for");

        // Since onDirectMessage is triggered when sending AND receiving a message,
        // we need to check who sends the message. This way, the bot doesn't
        // reply to itself or send duplicate messages
        if (!sentByBot && !wasGoogleSearch) {
            String sender = directMessage.getSenderScreenName();
            Log.i(TAG, "Message from @" + sender + ": " + message);
            // Check the message and response
            if (wantsQuote) {
                utils.sendMessage(sender, quotes[new Random().nextInt(NUM_QUOTES)]);
            } else if (wasGreeting) {
                utils.sendMessage(sender, botGreetings[new Random().nextInt(NUM_GREETING)]);
            } else if (wantsJoke) {
                utils.sendMessage(sender, "Sorry, I don't know any good jokes yet!");
            } else if (wantsRandomWiki) {
                utils.sendMessage(sender, "Check out this Wiki page:\nhttps://en.wikipedia.org/wiki/Special:Random");
            } else {
                utils.sendMessage(sender, misunderstandings[new Random().nextInt(NUM_MISUNDERSTANDINGS)]);
            }
        } else if (!sentByBot) {
            // Logic to search Google. This is triggered when the user tells the bot:
            // Search Google for...
            String sender = directMessage.getSenderScreenName();
            Log.i(TAG, "Message from @" + sender + ": " + message);
            // Split the sentence to store it in an array
            String[] searchQuery = message.split("\\s+");
            // We only care about the words after 'for...'
            String searchWord = searchQuery[searchQuery.length - 1];
            if (searchQuery.length == 4) {
                // Handles a one word search, i.e.: 'Search Google for cats'
                utils.sendMessage(sender, "Searching Google for " + searchWord + "...");
                utils.sleep(2000);
                utils.sendMessage(sender, "Here's what I found:\nhttps://www.google.com/search?q=" + searchWord);
            } else {
                // Handles a multi word search, i.e: 'Search Google for Kayne West'
                String searchWords = message.replace("search google for ", "");
                utils.sendMessage(sender, "Searching Google for " + searchWords);
                searchWords = searchWords.replace(" ", "+");
                utils.sleep(2000);
                utils.sendMessage(sender, "Here's what I found:\nhttps://www.google.com/search?q=" + searchWords);
            }
        }
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
        wordCount++;
        Log.d(TAG, "Occurrences of '" + keyWord + "': " + wordCount);
        intentUpdateUI.putExtra("number", wordCount);
        // Send the word count to the OccurrencesFragment so that the UI updates
        context.sendBroadcast(intentUpdateUI);

        // This formats the date to appear as: Mon, February 5, 2018 01:27 AM
        SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE, MMMM d, yyyy hh:mm aaa");

        Log.d(TAG, "@" + status.getUser().getScreenName());
        Log.d(TAG, "Created: " + dateFormatter.format(status.getCreatedAt()));
        Log.d(TAG, status.getText() + "\n");

    }

    public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {

    }

    public void onTrackLimitationNotice(int i) {
        Log.i(TAG, "Limitation: " + i);
    }

    public void onScrubGeo(long l, long l1) {

    }

    public void onStallWarning(StallWarning stallWarning) {
        Log.i(TAG, "STALLING, " + stallWarning.getMessage());
    }

    public void onException(Exception error) {
        Log.i(TAG, "onException... Stopping service");
        context.stopService(new Intent(context, TwitterService.class));

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "Exception");

        builder.setStyle(new NotificationCompat.BigTextStyle());

        builder.setSmallIcon(R.drawable.ic_stat_message);
        builder.setContentTitle("TwitterStream");
        builder.setColor(context.getColor(R.color.colorNotificationError));
        builder.setPriority(NotificationManager.IMPORTANCE_HIGH);

        // If there's no mobile data, we want to only show
        // the notification in the TwitterService
        if (!error.getMessage().contains("Unable to resolve host")) {
            // Error 402 occurs when there are too many auth
            // requests in a short amount of time
            if (error.getMessage().contains("420")) {
                builder.setContentText("Error " + error.getMessage() + "\n\nPlease wait 30 seconds before trying again");
            } else {
                builder.setContentText("Error " + error.getMessage());
            }
            // Don't vibrate if the user's device is on silent
            if (audio != null) {
                switch (audio.getRingerMode()) {
                    case AudioManager.RINGER_MODE_NORMAL:
                    case AudioManager.RINGER_MODE_VIBRATE:
                        builder.setVibrate(new long[]{0, 200, 200, 200});
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
    /**
     * AsyncTask to load the username of the bot. This is so that
     * the username will update if the bots username ever changes.
     * */
    @SuppressLint("StaticFieldLeak")
    public class ConnectionUtils extends AsyncTask<Void, Void, String> {
        String userName;

        @Override
        protected String doInBackground(Void... voids) {
            try {
                userName = TwitterUtils.setUpTwitter().getScreenName();
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            botScreenName = userName;
            Log.i(TAG, "onPostExecute: @" + botScreenName);
        }
    }
}
