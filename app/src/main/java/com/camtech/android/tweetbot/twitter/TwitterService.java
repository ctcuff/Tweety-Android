package com.camtech.android.tweetbot.twitter;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.activities.MainActivity;

import twitter4j.ConnectionLifeCycleListener;
import twitter4j.FilterQuery;
import twitter4j.Twitter;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;

/**
 * Service to start the bot. The connection is kept
 * alive while the service is running.
 */

public class TwitterService extends Service {

    private final String TAG = TwitterService.class.getSimpleName();
    private TwitterStream twitterStream;
    private static final String PREF_KEYWORD = "prefKeyword";
    private static final String DEFAULT_KEYWORD = "hello world";
    public static final String BROADCAST_UPDATE = "updateServiceStatus";
    final String PREF_NUM_OCCURRENCES = "prefOccurrences";
    private final String INTENT_STOP_SERVICE = "stopService";
    public static final int ID_BOT_CONNECTED = 0;
    final int ID_CONNECTION_LOST = 1;
    private String mode;
    SharedPreferences keywordPref;
    String keyWord;
    StreamListener listener;
    NotificationManager notificationManager;
    NotificationCompat.Builder builder;
    ConnectivityReceiver connectivityReceiver;
    TwitterUtils utils;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Receiver to listen for network changes
        connectivityReceiver = new ConnectivityReceiver();
        registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        registerReceiver(stopServiceReceiver, new IntentFilter(INTENT_STOP_SERVICE));
        utils = new TwitterUtils();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sendBroadcast(new Intent(BROADCAST_UPDATE));
        if (intent != null) mode = intent.getStringExtra(Intent.EXTRA_TEXT);

        keywordPref = getSharedPreferences(PREF_KEYWORD, MODE_PRIVATE);
        keyWord = keywordPref.getString(PREF_KEYWORD, DEFAULT_KEYWORD);

        Twitter twitter = TwitterUtils.setUpBot().getInstance();
        twitterStream = new TwitterStreamFactory(TwitterUtils.setUpConfig()).getInstance();

        if (mode != null && mode.equals("Occurrences")) {
            // Used to listen for a specific word or phrase
            listener = new StreamListener(this, twitter, keyWord);
            // Set a filter for the keyword to track its occurrences
            FilterQuery filter = new FilterQuery();
            filter.track(keyWord);
            twitterStream.addListener(listener);
            twitterStream.filter(filter);
        } else {
            // Used to listen for status updates, replies, messages, etc..
            // Mostly various user responses
            listener = new StreamListener(this, twitter);
            twitterStream.addListener(listener);
            twitterStream.user();
        }

        // Intent to open the OccurrencesFragment when the "OPEN" button is clicked
        Intent openActivityIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this)
                .addParentStack(MainActivity.class)
                .addNextIntent(openActivityIntent);
        PendingIntent openActivity = taskStackBuilder.getPendingIntent(100, PendingIntent.FLAG_UPDATE_CURRENT);

        builder = new NotificationCompat.Builder(this, "TwitterService");
        // Construct the notification such that it will show all text when swiped down
        builder.setStyle(new NotificationCompat.BigTextStyle());
        builder.setOngoing(false);
        builder.setShowWhen(false);
        builder.setSmallIcon(R.drawable.ic_stat_message);
        builder.setContentTitle("Tweet Bot");
        builder.addAction(R.drawable.ic_stat_message, "OPEN", openActivity);
        builder.setPriority(NotificationManager.IMPORTANCE_HIGH);
        builder.setVibrate(new long[]{});
        builder.setOngoing(true);
        if (mode != null && mode.equals("Occurrences")) {
            builder.setColor(getColor(R.color.colorOccurrences));
            builder.setUsesChronometer(true);
            builder.setContentText("Listening for occurrences of \"" + keyWord + "\". Touch to stop");
        } else {
            builder.setColor(getColor(R.color.colorMessages));
            builder.setContentText("Stream connected, listening for messages. Touch to stop");
        }

        // Intent to stop the service when the notification is clicked
        PendingIntent pendingIntent = PendingIntent.getBroadcast
                (this, ID_BOT_CONNECTED, new Intent(INTENT_STOP_SERVICE), PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (mode.equals("Occurrences")) notificationManager.notify(ID_BOT_CONNECTED, builder.build());
        twitterStream.addConnectionLifeCycleListener(new ConnectionLifeCycleListener() {
            @Override
            public void onConnect() {
                // This makes sure the notification only pops up on a successful connection
                if (mode.equals("Message")) notificationManager.notify(ID_BOT_CONNECTED, builder.build());
            }

            @Override
            public void onDisconnect() {
                Log.i(TAG, "onDisconnect...");
            }

            @Override
            public void onCleanUp() {

            }
        });

        // Only want the bot to restart when it's listening for direct messages
        if (mode != null && mode.equals("Occurrences")) return START_NOT_STICKY;
        else return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy... Shutting down");
        notificationManager.cancel(ID_BOT_CONNECTED);
        sendBroadcast(new Intent(BROADCAST_UPDATE));
        unregisterReceiver(stopServiceReceiver);
        unregisterReceiver(connectivityReceiver);
        twitterStream.cleanUp();
        twitterStream.shutdown();

        // Save the keyword and number of occurrences into a HashMap
        SharedPreferences numOccurrencesPref = getSharedPreferences(PREF_NUM_OCCURRENCES, MODE_PRIVATE);
        keyWord = keywordPref.getString(PREF_KEYWORD, DEFAULT_KEYWORD);
        int numOccurrences = numOccurrencesPref.getInt(PREF_NUM_OCCURRENCES, 0);
        utils.saveHashMap(keyWord, numOccurrences);
    }

    public boolean hasConnection() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (manager != null) {
            networkInfo = manager.getActiveNetworkInfo();
        }

        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    /**
     * Triggers when the notification is clicked; used to stop the service
     */
    private BroadcastReceiver stopServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopSelf();
        }
    };

    /**
     * Receiver to listen for changes in network connection.
     * Since this isn't registered in the Manifest, this receiver
     * only lives within the lifecycle of the service.
     */
    public class ConnectivityReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Mobile connection has dropped so we need to stop the service
            if (intent.getAction() != null && intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                if (!hasConnection()) {
                    builder = new NotificationCompat.Builder(context, "ConnectivityReceiver");
                    builder.setContentTitle("TwitterStream");
                    builder.setSmallIcon(R.drawable.ic_stat_message);
                    builder.setContentText("Error connecting to stream");
                    builder.setPriority(NotificationManager.IMPORTANCE_HIGH);
                    // The vibration pattern is {delay, vibrate, sleep, vibrate}
                    builder.setVibrate(new long[]{0, 200, 200, 200});
                    builder.setColor(getColor(R.color.colorNotificationError));
                    notificationManager.notify(ID_CONNECTION_LOST, builder.build());
                    stopSelf();
                }
            }
        }
    }
}
