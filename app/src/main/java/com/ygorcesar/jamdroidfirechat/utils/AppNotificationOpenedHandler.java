package com.ygorcesar.jamdroidfirechat.utils;

import android.util.Log;

import com.google.gson.Gson;
import com.onesignal.OneSignal;

import org.json.JSONObject;

public class AppNotificationOpenedHandler implements OneSignal.NotificationOpenedHandler {
    private static final String TAG = "NotificationOpened";

    @Override
    public void notificationOpened(String message, JSONObject additionalData, boolean isActive) {
        AditionalNotificationData aditionalData = new Gson().fromJson(additionalData.toString(),
                AditionalNotificationData.class);
        if (!isActive) {
            Utils.setAditionalData(aditionalData);
            Log.d(TAG, "notificationOpened: App em Background");
        }
    }
}
