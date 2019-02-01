package com.chat.app.mychatapp.main;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chat.app.mychatapp.Chat.ChatActivity;
import com.chat.app.mychatapp.Chat.Message;
import com.chat.app.mychatapp.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;
import static com.chat.app.mychatapp.DrawCalculations.bimapToTempFile;
import static com.chat.app.mychatapp.DrawCalculations.getBitmapFromURL;

public class ConversationsFragment extends Fragment {

    public static final int PRIVATE_CHAT = 1;
    private RecyclerView.Adapter adapter;
    private  List<Conversation> conversationList;
    private RecyclerView conversationView;
    private Handler handler;
    private static String ip;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_conversations, container, false);
        return rootView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        conversationList = new ArrayList<>();

        handler = new Handler(msg -> handleMessage());
        //BroadCast to update contact names from what was brought in the contacts fragment
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(
                contactsReload,  new IntentFilter("reloadConversations"));
        //Broadcast tto update the last the message of the some conversation
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(
                mMessageReceiver, new IntentFilter("newMessageIntent"));


    }

    @Override
    public void onStart() {
        super.onStart();
        ip = getActivity().getSharedPreferences(SettingsDialog.PREFS, MODE_PRIVATE).getString(SettingsDialog.IP, SettingsDialog.DEFAULT_IP);

        pullConversations();
        conversationView = getView().findViewById(R.id.conversationsRecyclerView);
        adapter = new ConversationsRecyclerViewAdapter(conversationList, i -> {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("conversation", conversationList.get(i));
            if(conversationList.get(i).getConversationPic() != null) {
                String filePath = bimapToTempFile(getContext(), conversationList.get(i).getConversationPic(),"image");
                intent.putExtra("image", filePath);
            }
            startActivity(intent);
            getActivity().overridePendingTransition(R.transition.enter_forward, R.transition.exit_forward);
            Objects.requireNonNull(getActivity()).finish();
        });
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        conversationView.setLayoutManager(layoutManager);
        conversationView.setAdapter(adapter);

    }


    private void pullConversations(){
        new Thread(() -> {
            conversationList.clear();
            URL url = null;
            HttpURLConnection connection = null;
            OutputStream outputStream = null;
            try {
                Log.d("Yan", ip);
                String urlString = "http://" + ip + "/ChatApp_war_exploded/request?option=pull_conversations";
                url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestProperty("Accept-Charset", "UTF-8" );
                connection.setRequestProperty("charset", "UTF-8" );
                connection.setRequestMethod("POST" );
                connection.setUseCaches(false);
                JSONObject jsonObject = new JSONObject();
                try {

                    String phoneNumber = getContext().getSharedPreferences(SettingsDialog.PREFS, MODE_PRIVATE).getString(SettingsDialog.PHONE_NUMBER, "");
                    if (phoneNumber.equals(""))
                        return;
                    jsonObject.put("phone_number", phoneNumber);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                outputStream = connection.getOutputStream();
                outputStream.write(jsonObject.toString().getBytes());
                connection.connect();
                    InputStream input = connection.getInputStream();
                    byte[] buffer = new byte[1024];
                    StringBuilder stringBuilder = new StringBuilder();
                    int actuallyRead;
                    while ((actuallyRead = input.read(buffer)) != -1) {
                        stringBuilder.append(new String(buffer, 0, actuallyRead, "UTF-8" ));
                    }
                    JSONObject jsonConversations = new JSONObject(stringBuilder.toString());
                    JSONArray jsonArray = jsonConversations.getJSONArray("conversations" );
                    for (int i = 0; i < jsonArray.length(); i++) {
                        Conversation conversation = new Conversation(jsonArray.getJSONObject(i));
                        if(conversation.getConversationPic() == null){
                            //conversation.setConversationPic(BitmapFactory.decodeResource(R.drawable.no_picture));
                        }
                        conversationList.add(conversation);
                        conversation.setConversationPic(getBitmapFromURL("http://"+ip+"/ChatApp_war_exploded/request?option=ask_for_image&user="+conversationList.get(i).getConversationTopic()));
                    }
                    if (handler != null) {
                        handler.sendMessage(new android.os.Message());
                    }
            } catch (JSONException e) {
                e.printStackTrace();
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


    private BroadcastReceiver contactsReload = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handler.sendMessage(new android.os.Message());
        }
    };
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            Message message = (Message) intent.getSerializableExtra("newReceivedMessage");
            if (conversationList != null && adapter != null && conversationView != null) {
                for (int i = 0; i < conversationList.size(); i++) {
                    if(conversationList.get(i).getUid() == message.getConversationID()){
                        conversationList.get(i).setLastMessage(message);
                        adapter.notifyDataSetChanged();
                        break;
                    }
                }
            }
        }

    };

    private boolean handleMessage() {
        if (adapter != null) {
            if(getActivity() != null){
                Set<String> contacts = getActivity().getSharedPreferences(SettingsDialog.PREFS, MODE_PRIVATE).getStringSet(ContactsFragment.CONTACTS, null);
                if (contacts != null) {
                    List<Contact> contactList = new ArrayList<>();
                    for (String contactAsString : contacts) {
                        contactList.add(new Contact(contactAsString));
                    }
                    if (contacts != null) {
                        for (int i = 0; i < conversationList.size(); i++) {
                            if (conversationList.get(i).getConversationType() == PRIVATE_CHAT) {
                                for (int j = 0; j < contactList.size(); j++) {
                                    if (contactList.get(j).phoneNumber.equals(conversationList.get(i).getConversationTopic())) {
                                        conversationList.get(i).setConversationTopic(contactList.get(j).name);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            adapter.notifyDataSetChanged();
        }
        return true;
    }
}