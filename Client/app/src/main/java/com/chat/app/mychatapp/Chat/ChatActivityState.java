package com.chat.app.mychatapp.Chat;

import android.app.Application;

public class ChatActivityState extends Application {

    private static boolean activityVisible;
    private static int conversationID;
    private  static boolean activityOnDestroy = true;
    public static boolean isActivityVisible() {
        return activityVisible;
    }
    public static boolean isActivityOnDestroy(){
        return activityOnDestroy;
    }

    public static void activityResumed() {
        activityVisible = true;
        activityOnDestroy = false;
    }

    public static void activityPaused() {
        activityVisible = false;
    }

    public static void activityOnDestroy() { activityOnDestroy = true;}


    public static int getConversationID() {
        return conversationID;
    }

    public static void setConversationID(int conversationID) {
       ChatActivityState.conversationID = conversationID;
    }
}
