package com.chat.app.mychatapp.Chat;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chat.app.mychatapp.R;
import com.chat.app.mychatapp.main.Conversation;
import com.chat.app.mychatapp.main.MainActivity;
import com.chat.app.mychatapp.main.SettingsDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.view.View.GONE;
import static com.chat.app.mychatapp.DrawCalculations.convertDpToPixel;
import static com.chat.app.mychatapp.main.SettingsDialog.DEFAULT_IP;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ChatActivity extends AppCompatActivity {

    public static final int SCROLL_DOWN = 100;
    public static final int BACKGROUND = 101;
    public static final int FULL_BACKGROUND = 102;
    private EditText chatText;
    private ImageView cameraBtn;
    private ImageView attachmentBtn;
    private Button sendBtn;
    private boolean beganWriting;
    private String reciverPhoneNumber;
    private RecyclerView.LayoutManager layoutManager;
    private String sender;
    private List<Message> messageList = new ArrayList<>();
    private ChatRecyclerViewAdapter adapter;
    private RecyclerView recyclerView;
    private Handler handler;
    private LinearLayout mainLayout;
    private int conversationID = -1;
    private static String ip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        mainLayout = findViewById(R.id.main_chat_layout);
        ip = getSharedPreferences(SettingsDialog.PREFS, MODE_PRIVATE).getString(SettingsDialog.IP, DEFAULT_IP);
        sender = getSharedPreferences(SettingsDialog.PREFS, MODE_PRIVATE).getString(SettingsDialog.PHONE_NUMBER, "");
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(android.os.Message msg) {
               if(msg.arg1 == BACKGROUND){
                    mainLayout.setBackground(getResources().getDrawable(R.drawable.small_background, getTheme()));
                    if(msg.arg2 == SCROLL_DOWN)
                        recyclerView.scrollToPosition(messageList.size() -1);
                }else if(msg.arg1 == FULL_BACKGROUND){
                    mainLayout.setBackground(getResources().getDrawable(R.drawable.background, getTheme()));
                }else {
                   if (adapter != null) {

                       adapter.notifyDataSetChanged();
                       recyclerView.scrollToPosition(messageList.size() - 1);
                   }
               }
                return true;
            }
        });
        //getSharedPreferences(SettingsDialog.PREFS, MODE_PRIVATE).edit().putString(SettingsDialog.PHONE_NUMBER, sender).commit();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("newMessageIntent"));

        Intent intent = getIntent();
        if(intent.hasExtra("receiverPhoneNumber")){
            reciverPhoneNumber = intent.getStringExtra("receiverPhoneNumber");
        }
        if(intent.hasExtra("conversation")) {
            //Pull conversation uid + title + image
            Conversation conversation = (Conversation) intent.getSerializableExtra("conversation");
            conversationID = conversation.getUid();
            ChatActivityState.setConversationID(conversationID);


            TextView conversationTitle = findViewById(R.id.conversationTitle);
            conversationTitle.setText(conversation.getConversationTopic());
            if(intent.hasExtra("image")) {

                String path = intent.getStringExtra("image");
                File file = new File(path);
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                conversation.setConversationPic(bitmap);
                ImageView conversationPicture = findViewById(R.id.conversation_pic);
                conversationPicture.setImageBitmap(conversation.getConversationPic());
            }
            pullAllConversationMessagesFromServer(conversation.getUid());
        }
        cameraBtn = findViewById(R.id.camera_btn);
        attachmentBtn = findViewById(R.id.attachment_btn);
        chatText = findViewById(R.id.chatInputText);
        sendBtn = findViewById(R.id.send_btn);
        chatText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                float moveBy = convertDpToPixel(43, ChatActivity.this);
                Locale primaryLocale = getResources().getConfiguration().getLocales().get(0);
                String locale = primaryLocale.getDisplayName();
                if(locale.contains("עברית"))
                    moveBy = -moveBy;
                if(!beganWriting && chatText.getText().length() > 0){
                    beganWriting = true;
                    cameraBtn.animate().translationX(moveBy).alpha(0.0f).setDuration(100).withEndAction(()->cameraBtn.setVisibility(GONE));
                    attachmentBtn.animate().translationX(moveBy).setDuration(100);

                }else if(beganWriting && chatText.getText().length() == 0){
                    beganWriting = false;
                    cameraBtn.setVisibility(View.VISIBLE);
                    cameraBtn.animate().translationX(0).alpha(1.0f).setDuration(100);
                    attachmentBtn.animate().translationX(0).setDuration(100);
                }
            }
        });
        recyclerView = findViewById(R.id.messagesRecyclerView);
        adapter = new ChatRecyclerViewAdapter(messageList);
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
       // ((LinearLayoutManager) layoutManager).setStackFromEnd(true);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        sendBtn.setOnClickListener(v -> {
            if(beganWriting){
                Time time = new Time(System.currentTimeMillis());
                String currentTime = time.toString();
                String[] splitParts = currentTime.split(":");
                currentTime = splitParts[0] + ":" + splitParts[1];
                Log.d("Yan" , ""+(int)chatText.toString().charAt(0)+" "+(int)chatText.toString().charAt(1));

                Message message = new Message(removeSpacesFromTheEnd(chatText.getText().toString()), currentTime, conversationID ,false);
                message.setSender(sender);
                if(message.getMessageBody() != null){
                    messageList.add(message);
                    chatText.setText("");
                    adapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(messageList.size() - 1);
                    if(message.getConversationID() != -1) {
                        sendNewMessageToServer(message);
                    }else{
                        if(reciverPhoneNumber.isEmpty())
                            finish();
                        //new Conversations
                        sendMessageNewConversations(message, reciverPhoneNumber);
                    }
                }

            }
        });

        recyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if(bottom < oldBottom){
                    android.os.Message message = new android.os.Message();
                    message.arg1 = BACKGROUND;
                    if(((LinearLayoutManager) layoutManager).findLastVisibleItemPosition() > messageList.size()-7)
                        message.arg2 = SCROLL_DOWN;
                    handler.sendMessage(message);


                }
                if(oldBottom < bottom){
                    android.os.Message message = new android.os.Message();
                    message.arg1 = FULL_BACKGROUND;
                    handler.sendMessage(message);
                }
            }
        });


    }


    public void sendNewMessageToServer(Message msg){
        MessageRunnable messageThread = new MessageRunnable(msg) {
            @Override
            public void run() {
                URL url = null;
                HttpURLConnection connection = null;
                OutputStream outputStream = null;
                try {
                    Log.d("Yan", "began");
                    url = new URL("http://"+ip+ "/ChatApp_war_exploded/request?option=new_message");
                    connection =(HttpURLConnection) url.openConnection();
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Accept-Charset", "UTF-8");
                    connection.setRequestProperty("charset", "UTF-8");
                    connection.setRequestMethod("POST");
                    connection.setUseCaches(false);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("message", this.runnableMessage.toJSON());
                    outputStream = connection.getOutputStream();
                    outputStream.write(jsonObject.toString().getBytes());
                    connection.connect();
                    Log.d("Yan", ""+connection.getResponseCode());

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                            Log.d("Yan", "outputtstream closed");

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (connection != null) {
                        connection.disconnect();
                        Log.d("Yan", "connection closed");
                    }
                }

            }
        };

        new Thread(messageThread).start();
    }

    public void sendMessageNewConversations(Message msg, String reciverPhoneNumber){
        MessageRunnable messageThread = new MessageRunnable(msg, reciverPhoneNumber) {
            @Override
            public void run() {
                URL url = null;
                HttpURLConnection connection = null;
                OutputStream outputStream = null;
                try {
                    Log.d("Yan", "began");
                    url = new URL("http://"+ip+ "/ChatApp_war_exploded/request?option=new_message");
                    connection =(HttpURLConnection) url.openConnection();
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Accept-Charset", "UTF-8");
                    connection.setRequestProperty("charset", "UTF-8");
                    connection.setRequestMethod("POST");
                    connection.setUseCaches(false);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("message", this.runnableMessage.toJSON());
                    jsonObject.put("secondNumber", this.reciver);
                    outputStream = connection.getOutputStream();
                    outputStream.write(jsonObject.toString().getBytes());
                    connection.connect();
                    Log.d("Yan", ""+connection.getResponseCode());

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                            Log.d("Yan", "outputtstream closed");

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (connection != null) {
                        connection.disconnect();
                        Log.d("Yan", "connection closed");
                    }
                }

            }
        };
        new Thread(messageThread).start();
    }
    public void pullAllConversationMessagesFromServer(int conversationID){
        MessagePullRunnable messagePullRunnable = new MessagePullRunnable(conversationID) {
            @Override
            public void run() {
                URL url = null;
                HttpURLConnection connection = null;
                OutputStream outputStream = null;
                try {
                    Log.d("Yan", "began");
                    url = new URL("http://" +ip+ "/ChatApp_war_exploded/request?option=pull_conversation_messages");
                    connection =(HttpURLConnection) url.openConnection();
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    connection.setRequestProperty("Accept-Charset", "UTF-8");
                    connection.setRequestProperty("charset", "UTF-8");
                    connection.setRequestMethod("POST");
                    connection.setUseCaches(false);
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("conversation_id", conversationID);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    outputStream = connection.getOutputStream();
                    outputStream.write(jsonObject.toString().getBytes());
                    connection.connect();
                    BufferedReader in = null;

                    try {
                        Log.d("Yan", "getting input stream");
                        InputStream input = connection.getInputStream();
                        in = new BufferedReader(new InputStreamReader(input, UTF_8));
                        char[] buffer = new char[1024];
                        StringBuilder stringBuilder = new StringBuilder();
                        int actuallyRead;
                        while((actuallyRead = in.read(buffer)) != -1){
                            stringBuilder.append(new String(buffer, 0, actuallyRead));
                        }
                        JSONObject jsonMessages = new JSONObject(stringBuilder.toString());
                        JSONArray jsonArray = jsonMessages.getJSONArray("messages");
                        messageList.clear();
                        for (int i = 0; i <jsonArray.length() ; i++) {
                            Message message = new Message(jsonArray.getJSONObject(i));
                            if(message.getSender().equals(sender)){
                                message.setReceiver(false);
                            }else{
                                message.setReceiver(true);
                            }
                            Log.d("Yan", ""+message.isMessagedRead());
                            messageList.add(message);
                        }
                        if(handler != null){
                            handler.sendMessage(new android.os.Message());
                        }
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
                            Log.d("Yan", "outputtstream closed");

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (connection != null) {
                        connection.disconnect();
                        Log.d("Yan", "connection closed");
                    }
                }
            }
        };
        new Thread(messagePullRunnable).start();
    }
    @Override
    protected void onPause() {
        ChatActivityState.activityPaused();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ChatActivityState.activityResumed();
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(conversationID);
        getSharedPreferences(SettingsDialog.PREFS, Context.MODE_PRIVATE).edit().putStringSet("notification"+conversationID, null).apply();

    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.transition.enter_backward, R.transition.exit_backward);
        finish();
    }

    public static String removeSpacesFromTheEnd(String str){
        int firstSpace=0;
        boolean first=false;
        for (int i = 0; i < str.length() ; i++) {
            if((int) str.charAt(i) == 32|| (int) str.charAt(i) == 10){
                if(!first){
                    firstSpace = i;
                    first=true;
                }
            }else{
                firstSpace = 0;
                first=false;
            }
        }

        if(firstSpace != 0) {
            str = str.substring(0, firstSpace);
            return str;
        }if(!first) {
            return str;
        }
        return null;
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Message message = (Message) intent.getSerializableExtra("newReceivedMessage");
            if (messageList != null && adapter != null && recyclerView != null) {
                messageList.add(message);
                adapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(messageList.size() - 1);

            }
        }

    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ChatActivityState.activityOnDestroy();
    }

    abstract class MessageRunnable implements Runnable{
        public Message runnableMessage;
        public String reciver;
        public MessageRunnable(Message message){
            this.runnableMessage = message;
        }
        public MessageRunnable(Message message,String reciver){
            this.reciver = reciver;
            this.runnableMessage = message;
        }
    }

    abstract class MessagePullRunnable implements Runnable{
        public int conversationID;
        public MessagePullRunnable(int conversationID){
            this.conversationID = conversationID;
        }
    }
}
