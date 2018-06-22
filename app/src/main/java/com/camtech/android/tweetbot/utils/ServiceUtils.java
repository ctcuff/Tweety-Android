package com.camtech.android.tweetbot.utils;

import android.app.ActivityManager;
import android.content.Context;

import static android.app.ActivityManager.RunningServiceInfo;

public class ServiceUtils {

    /**
     * Used to determine if a given service is running
     * */
    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
