package com.unity3d.nostatusbar;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;

public class ResultActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Cancel Notification
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancel(10086);

        // TODO: Handle and display message/conversation from your database
        // NOTE: You can retrieve the EXTRA_REMOTE_INPUT_DRAFT sent by the system when a user
        // inadvertently closes a messaging notification to pre-populate the reply text field so
        // the user can finish their reply.
    }
}
