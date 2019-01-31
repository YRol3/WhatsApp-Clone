package com.chat.app.mychatapp.FCM;



import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import android.graphics.Bitmap;

import android.graphics.Color;
import android.os.Build;


import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.chat.app.mychatapp.Chat.ChatActivity;
import com.chat.app.mychatapp.R;
import com.chat.app.mychatapp.main.Conversation;
import com.chat.app.mychatapp.main.SettingsDialog;

import java.util.HashSet;
import java.util.Set;

import static com.chat.app.mychatapp.DrawCalculations.bimapToTempFile;
import static com.chat.app.mychatapp.DrawCalculations.getBitmap;
import static com.chat.app.mychatapp.DrawCalculations.getBitmapFromURL;
import static com.chat.app.mychatapp.DrawCalculations.getCircleBitmap;


public class Notificator {
    private static final String CHANNEL_ID = "12311";

    public void pushNotification(String ip,int uid, String title, String body, String sender, Context context){
        Set<String> notifications =  context.getSharedPreferences(SettingsDialog.PREFS, Context.MODE_PRIVATE).getStringSet("notification"+uid, null);
        if(notifications == null){
            notifications = new HashSet<>();
        }
        notifications.add(body);
        context.getSharedPreferences(SettingsDialog.PREFS, Context.MODE_PRIVATE).edit().putStringSet("notification"+uid, notifications).apply();
        Bitmap userPicture = getBitmapFromURL("http://"+ ip+"/ChatApp_war_exploded/request?option=ask_for_image&user="+sender);
        Intent intent = new Intent(context, ChatActivity.class);
        Conversation conversation = new Conversation(uid);
        conversation.setConversationTopic(title);
        intent.putExtra("conversation", conversation);
        if(userPicture != null){
            String filePath = bimapToTempFile(context, userPicture, "userPic");
            intent.putExtra("image", filePath);
        }
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        if(userPicture == null)
            userPicture = getBitmap(context, R.drawable.no_picture);
        createNotificationChannel(context);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setLargeIcon(getCircleBitmap(userPicture))
                    .setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(Notification.PRIORITY_MAX);

            mBuilder.build();

        if(notifications.size() > 1) {
            NotificationCompat.InboxStyle ns = new NotificationCompat.InboxStyle();
            for (String msg : notifications) {
                ns.addLine(msg);
            }
            ns.setSummaryText("+" + notifications.size() + " " + context.getString(R.string.more_messages_notification));
            mBuilder.setStyle(ns);
        }

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.notify(uid, mBuilder.build());
    }


    private void createNotificationChannel(Context context) {
        // check if sdk is oreo or bigger to create a notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.notification_channel_name);
            String description = context.getString(R.string.notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.shouldShowLights();
            channel.setVibrationPattern((new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400}));
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
