package com.camtech.android.tweetbot.utils;

import android.app.ActivityManager;
import android.content.Context;

public class ServiceUtils {

    /**
     * Checks if the given service is running
     * @return true if it is, false otherwise
     * */
    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
