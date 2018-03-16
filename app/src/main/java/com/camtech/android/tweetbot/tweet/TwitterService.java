package com.camtech.android.tweetbot.tweet;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.activities.MainActivity;
import com.camtech.android.tweetbot.fragments.OccurrencesFragment;

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
    private final String INTENT_STOP_SERVICE = "stopService";
    private final int ID_CONNECTION_LOST = 1;
    private String mode;
    private SharedPreferences keywordPref;
    private String keyWord;
    private StreamListener streamListener;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;
    private ConnectivityReceiver connectivityReceiver;
    private TwitterUtils utils;

    public static final String BROADCAST_UPDATE = "updateServiceStatus";
    public static final int ID_BOT_CONNECTED = 0;

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
        // Receiver to listen for when the notification is clicked
        registerReceiver(stopServiceReceiver, new IntentFilter(INTENT_STOP_SERVICE));
        utils = new TwitterUtils();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // This broadcast is sent to update the button text on each fragment
        sendBroadcast(new Intent(BROADCAST_UPDATE));

        // Determine which fragment started this service
        if (intent != null) mode = intent.getStringExtra(Intent.EXTRA_TEXT);

        keywordPref = getSharedPreferences(getString(R.string.pref_keyword), MODE_PRIVATE);
        keyWord = keywordPref.getString(getString(R.string.pref_keyword), getString(R.string.pref_default_keyword));

        Twitter twitter = TwitterUtils.setUpTwitter();
        twitterStream = new TwitterStreamFactory(TwitterUtils.getConfig()).getInstance();

        if (mode != null && mode.equals(OccurrencesFragment.OCCURRENCES)) {
            // Used to listen for a specific word or phrase
            streamListener = new StreamListener(this, twitter, keyWord);
            // Set a filter for the keyword to track its occurrences
            FilterQuery query = new FilterQuery(keyWord);
            query.track(keyWord);

            twitterStream.addListener(streamListener);
            twitterStream.filter(query);

        } else {
            // Used to listen for status updates, replies, messages, etc..
            // Mostly various user responses
            streamListener = new StreamListener(this, twitter);
            twitterStream.addListener(streamListener);
            twitterStream.user();
        }

        // Intent to open the OccurrencesFragment when the "OPEN" button is clicked
        Intent openActivityIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this)
                .addParentStack(MainActivity.class)
                .addNextIntent(openActivityIntent);
        PendingIntent openActivity = taskStackBuilder.getPendingIntent(100, PendingIntent.FLAG_UPDATE_CURRENT);

        builder = new NotificationCompat.Builder(this, "TwitterService");
        // Construct the notification to show all text when swiped down
        builder.setStyle(new NotificationCompat.BigTextStyle());
        builder.setShowWhen(false);
        builder.setAutoCancel(true);
        builder.setSmallIcon(R.drawable.ic_stat_message);
        builder.setContentTitle(getString(R.string.notification_title));
        builder.addAction(R.drawable.ic_stat_message, "OPEN", openActivity);
        builder.setPriority(NotificationManager.IMPORTANCE_HIGH);
        builder.setVibrate(new long[]{}); // If we don't include this, the notification won't drop down
        builder.setOngoing(true); //Notification can't be swiped away

        if (mode != null && mode.equals(OccurrencesFragment.OCCURRENCES)) {
            builder.setColor(getColor(R.color.colorOccurrences));
            builder.setUsesChronometer(true);
            builder.setContentText(getString(R.string.notification_stream_occurrences, keyWord));
        } else {
            builder.setColor(getColor(R.color.colorMessages));
            builder.setContentText(getString(R.string.notification_stream_message));
        }

        // Intent to stop the service when the notification is clicked
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                ID_BOT_CONNECTED,
                new Intent(INTENT_STOP_SERVICE),
                PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(pendingIntent);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(ID_BOT_CONNECTED, builder.build());
        twitterStream.addConnectionLifeCycleListener(new ConnectionLifeCycleListener() {
            @Override
            public void onConnect() {

            }

            @Override
            public void onDisconnect() {
                // This makes sure the notification is cancelled if/when the stream is closed
                notificationManager.cancel(ID_BOT_CONNECTED);
            }

            @Override
            public void onCleanUp() {

            }
        });

        // Only want the bot to restart when it's listening for direct messages
        if (mode != null && mode.equals(OccurrencesFragment.OCCURRENCES)) return START_NOT_STICKY;
        else return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        notificationManager.cancel(ID_BOT_CONNECTED);
        sendBroadcast(new Intent(BROADCAST_UPDATE));
        unregisterReceiver(stopServiceReceiver);
        unregisterReceiver(connectivityReceiver);
        twitterStream.cleanUp();
        twitterStream.shutdown();

        // Save the keyword and number of occurrences into a HashMap
        SharedPreferences numOccurrencesPref = getSharedPreferences(getString(R.string.pref_num_occurrences), MODE_PRIVATE);
        keyWord = keywordPref.getString(getString(R.string.pref_keyword), getString(R.string.pref_default_keyword));
        int numOccurrences = numOccurrencesPref.getInt(getString(R.string.pref_num_occurrences), 0);
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
            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (intent.getAction() != null && intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                if (!hasConnection()) {
                    builder = new NotificationCompat.Builder(context, "ConnectivityReceiver");
                    builder.setContentTitle(getString(R.string.notification_title));
                    builder.setSmallIcon(R.drawable.ic_stat_message);
                    builder.setContentText("Error connecting to stream, no mobile data");
                    builder.setPriority(NotificationManager.IMPORTANCE_HIGH);
                    // The vibration pattern is {delay, vibrate, sleep, vibrate}
                    builder.setColor(getColor(R.color.colorNotificationError));
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
                    notificationManager.notify(ID_CONNECTION_LOST, builder.build());
                    stopSelf();
                }
            }
        }
    }
}
