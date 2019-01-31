package com.chat.app.mychatapp.FCM;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.chat.app.mychatapp.Chat.ChatActivityState;
import com.chat.app.mychatapp.Chat.Message;
import com.chat.app.mychatapp.main.Contact;
import com.chat.app.mychatapp.main.ContactsFragment;
import com.chat.app.mychatapp.main.SettingsDialog;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import static com.chat.app.mychatapp.main.SettingsDialog.DEFAULT_IP;

public class FirebaseCouldMessageService extends FirebaseMessagingService {

    private String ip;
    Notificator notificator;

    @Override
    public void onCreate() {
        super.onCreate();
        notificator = new Notificator();

        ip = getSharedPreferences(SettingsDialog.PREFS, MODE_PRIVATE).getString(SettingsDialog.IP, DEFAULT_IP);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Message message = null;
        try {
            JSONObject jsonObject = new JSONObject(remoteMessage.getData().get("message"));
            message = new Message(jsonObject);
            message.setReceiver(true);
            String title = message.getSender();
            Set<String> contacts= getSharedPreferences(SettingsDialog.PREFS, MODE_PRIVATE).getStringSet(ContactsFragment.CONTACTS,null);
            if(contacts != null) {
                //Pull saved contacts from memory (If user uses the App && In User contacts they will come up
                for (String phoneNumber : contacts) {
                    Contact contact = new Contact(phoneNumber);
                    if (contact.getPhoneNumber().equals(message.getSender())) {
                        title = contact.getName();
                    }
                }
            }
            if (ChatActivityState.getConversationID() != message.getConversationID()) {
                //If user is not in the same conversation as the current conversations, Will send push notification
                notificator.pushNotification(ip, message.getConversationID(),  title, message.getMessageBody(), message.getSender(), this);
            } else if(ChatActivityState.getConversationID() == message.getConversationID()){
                //If the user in the same Conversations will send local broadCast
                Intent intent = new Intent("newMessageIntent");
                intent.putExtra("newReceivedMessage", message);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                if(!ChatActivityState.isActivityVisible())
                    //If the chat activity is in onPause will send push notificaiton anyway
                    notificator.pushNotification(ip, message.getConversationID(),  title, message.getMessageBody(), message.getSender(), this);
            }
            if(ChatActivityState.isActivityOnDestroy()){
                /*
                    This makes make sure if the chat activity came to onDestory
                    We could update the conversation list with the lastest message in the recycler view Row of the conversations
                 */
                Intent intent = new Intent("newMessageIntent");
                intent.putExtra("newReceivedMessage", message);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    @Override
    public void onNewToken(String s) {
        String phoneNumber = getSharedPreferences("phone", MODE_PRIVATE).getString("number", "");
        sendNewToken(s, ip, phoneNumber );
    }

    public static void sendNewToken(String token, String ip, String phoneNumber){
        new Thread(() ->{
            URL url = null;
            HttpURLConnection connection = null;
            OutputStream outputStream = null;
            try {
                url = new URL("http://" + ip + "/ChatApp_war_exploded/request?option=new_token");
                connection =(HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestProperty("Accept-Charset", "UTF-8");
                connection.setRequestProperty("charset", "UTF-8");
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("token", token);
                    jsonObject.put("phone_number", phoneNumber);
                    outputStream = connection.getOutputStream();
                    outputStream.write(jsonObject.toString().getBytes());
                    connection.connect();
                    connection.getResponseCode();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }
}
