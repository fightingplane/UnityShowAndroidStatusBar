package com.unity3d.nostatusbar;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

//import com.chaychan.pushdemo.activity.MainActivity;
//import com.chaychan.pushdemo.activity.MessageCenterActivity;
//import com.chaychan.pushdemo.activity.OrderDetailActivity;
//import com.chaychan.pushdemo.global.PushConstants;
//import com.chaychan.pushdemo.utils.SystemUtils;

import com.unity3d.player.UnityPlayer;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NotificationReceiver extends BroadcastReceiver {

    public static final String LogTag = NotificationReceiver.class.getSimpleName();
    private List<Intent> intentList;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LogTag, "some notification clicked");
        Bundle bundle = intent.getExtras();
        if (bundle != null)
        {
            String notificationString = bundle.getString("NotificationMessage");
            if (!notificationString.isEmpty())
            {
                Log.d(LogTag,"Notification Message Clicked: " + notificationString);
                UnityPlayerActivityStatusBar.ClickedNotification = notificationString;
                //send to unity
                UnityPlayer.UnitySendMessage("Universe", "onNotificationMessageClicked", notificationString);
            }
        }
    }
}