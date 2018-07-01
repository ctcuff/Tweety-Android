package com.camtech.android.tweetbot.services;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.activities.MainActivity;
import com.camtech.android.tweetbot.core.StreamListener;
import com.camtech.android.tweetbot.utils.DbUtils;
import com.camtech.android.tweetbot.utils.TwitterUtils;

import twitter4j.ConnectionLifeCycleListener;
import twitter4j.FilterQuery;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;

/**
 * Service to start the stream. The connection is kept
 * alive while the service is running.
 */
public class TwitterService extends Service {
    public static final String TAG = TwitterService.class.getSimpleName();
    private  TwitterStream twitterStream;
    private final String INTENT_STOP_SERVICE = "stopService";
    private String keyWord;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;
    private ConnectivityReceiver connectivityReceiver;
    private int occurrences;

    public static final String BROADCAST_UPDATE = "updateServiceStatus";
    public static final int ID_STREAM_CONNECTED = 0;
    public static final int ID_OPEN_MAIN_ACTIVITY = 100;

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
        registerReceiver(numOccurrencesReceiver, new IntentFilter(StreamListener.OCCURRENCES_INTENT_FILTER));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // This broadcast is sent to update the button text on the OccurrencesFragment
        sendBroadcast(new Intent(BROADCAST_UPDATE));
        keyWord = intent.getStringExtra(Intent.EXTRA_TEXT);

        Intent autoSaveIntent = new Intent(this, AutoSaveService.class);
        autoSaveIntent.putExtra(TAG, keyWord);
        startService(autoSaveIntent);

        // Set a filter for the keyword to track its occurrences
        FilterQuery query = new FilterQuery(keyWord);
        // Used to listen for a specific word or phrase
        StreamListener streamListener = new StreamListener(this, keyWord);
        twitterStream = new TwitterStreamFactory(TwitterUtils.getConfig(this)).getInstance();
        twitterStream.addListener(streamListener);
        twitterStream.filter(query);

        // Intent to open the OccurrencesFragment when the "OPEN" button is clicked
        Intent openActivityIntent = new Intent(this, MainActivity.class);
        // Opens the MainActivity but only if the activity is in the background.
        // Any further clicks won't do anything
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent openActivity = PendingIntent.getActivity(
                this,
                ID_OPEN_MAIN_ACTIVITY,
                openActivityIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        // Construct the notification to show all text when swiped down
        builder = new NotificationCompat.Builder(this, "TwitterService");
        builder.setStyle(new NotificationCompat.BigTextStyle());
        builder.setShowWhen(false);
        builder.setAutoCancel(false);
        builder.setOngoing(true);
        builder.setSmallIcon(R.drawable.ic_stat_message);
        builder.setContentTitle(getString(R.string.notification_title));
        builder.addAction(R.drawable.ic_stat_message, "OPEN", openActivity);
        builder.setVibrate(new long[]{}); // If we don't include this, the notification won't drop down
        builder.setColor(getResources().getColor(R.color.colorOccurrences));
        builder.setUsesChronometer(true);
        builder.setContentText(getString(R.string.notification_stream_occurrences, keyWord));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setPriority(NotificationManager.IMPORTANCE_HIGH);
        }

        // Intent to stop the service when the notification is clicked
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                ID_STREAM_CONNECTED,
                new Intent(INTENT_STOP_SERVICE),
                PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(pendingIntent);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(ID_STREAM_CONNECTED, builder.build());
        twitterStream.addConnectionLifeCycleListener(new ConnectionLifeCycleListener() {
            @Override
            public void onConnect() {

            }

            @Override
            public void onDisconnect() {
                // This makes sure the notification is cancelled if/when the stream is closed
                notificationManager.cancel(ID_STREAM_CONNECTED);
            }

            @Override
            public void onCleanUp() {

            }
        });

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanUpAndSave();
        notificationManager.cancel(ID_STREAM_CONNECTED);
        sendBroadcast(new Intent(BROADCAST_UPDATE));
        unregisterReceiver(stopServiceReceiver);
        unregisterReceiver(connectivityReceiver);
        unregisterReceiver(numOccurrencesReceiver);
        stopService(new Intent(this, AutoSaveService.class));
    }

    public boolean hasConnection() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (manager != null) {
            networkInfo = manager.getActiveNetworkInfo();
        }

        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    @SuppressLint("StaticFieldLeak")
    private void cleanUpAndSave() {
        // Sometimes stopping the stream freezes the UI so we need
        // to stop it using an AsyncTask. We can also use this to
        // save data to the database
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                twitterStream.cleanUp();
                twitterStream.shutdown();
                DbUtils.saveKeyWord(getBaseContext(), keyWord, occurrences);
                return null;
            }
        }.execute();
    }

    private BroadcastReceiver numOccurrencesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            occurrences = intent.getIntExtra(StreamListener.NUM_OCCURRENCES_EXTRA, 0);
        }
    };

    /**
     * Triggers when the notification itself  is clicked; used to stop the service
     */
    private BroadcastReceiver stopServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopSelf();
            Intent timerIntent = new Intent(getBaseContext(), TimerService.class);
            stopService(timerIntent);
            startService(timerIntent);
        }
    };

    /**
     * Receiver to listen for changes in network connection.
     * Since this isn't registered in the Manifest, this receiver
     * only lives within the lifecycle of the service.
     */
    public class ConnectivityReceiver extends BroadcastReceiver {
        private final int ID_CONNECTION_LOST = 1;
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
                    builder.setColor(getResources().getColor(R.color.colorNotificationError));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        builder.setPriority(NotificationManager.IMPORTANCE_HIGH);
                    }
                    // Don't vibrate if the user's device is on silent
                    if (audio != null) {
                        switch (audio.getRingerMode()) {
                            case AudioManager.RINGER_MODE_NORMAL:
                            case AudioManager.RINGER_MODE_VIBRATE:
                                // The vibration pattern is {delay, vibrate, sleep, vibrate}
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
