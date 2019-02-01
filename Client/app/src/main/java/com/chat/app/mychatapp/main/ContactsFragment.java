package com.chat.app.mychatapp.main;


import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.chat.app.mychatapp.Chat.ChatActivity;
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
import java.nio.ByteBuffer;
import java.util.ArrayList;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static com.chat.app.mychatapp.DrawCalculations.bimapToTempFile;
import static com.chat.app.mychatapp.DrawCalculations.getBitmapFromURL;


public class ContactsFragment extends Fragment {

    public static final String CONTACTS = "contacts";
    public static final int REQUEST_CODE = 5;
    private List<Contact> contacts;
    private RecyclerView contactsView;
    private LinearLayout permissionLayout;
    private Button permissionButton;
    private RecyclerView.LayoutManager layoutManger;
    private RecyclerView.Adapter adapter;
    private Handler handler;
    private String ip;
    private String sender;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_contacts, container, false);
        contactsView = rootView.findViewById(R.id.contactsRecyclerView);
        permissionLayout = rootView.findViewById(R.id.permissionLayout);
        permissionButton = rootView.findViewById(R.id.permissionRequest);
        layoutManger = new LinearLayoutManager(getContext());
        contactsView.setLayoutManager(layoutManger);
        contactsView.setAdapter(adapter);
        return rootView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sender = getActivity().getSharedPreferences(SettingsDialog.PREFS, MODE_PRIVATE).getString(SettingsDialog.PHONE_NUMBER, "");
        if(sender.equals(""))
            return;
        contacts = new ArrayList<>();
        ip = getActivity().getSharedPreferences(SettingsDialog.PREFS, MODE_PRIVATE).getString(SettingsDialog.IP, SettingsDialog.DEFAULT_IP);
        Set<String> contactsString = getActivity().getSharedPreferences(SettingsDialog.PREFS, Context.MODE_PRIVATE).getStringSet(CONTACTS, null);
        if(contactsString != null){
            for(String contact: contactsString){
                contacts.add(new Contact(contact));
            }
        }

        adapter = new ContactsRecyclerViewAdapter(contacts, position ->{
            String phoneNumber = contacts.get(position).getPhoneNumber();
            mysqlGetConversationID(phoneNumber, sender, position, (uid, i) ->{
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                Conversation conversation = new Conversation(uid);
                conversation.setConversationTopic(contacts.get(i).getName());
                intent.putExtra("conversation", conversation);
                if(contacts.get(i).getPicture() != null){
                    String filePath = bimapToTempFile(getContext(), contacts.get(i).getPicture(),"image");
                    intent.putExtra("image", filePath);
                }
                intent.putExtra("receiverPhoneNumber", phoneNumber);
                startActivity(intent);
                getActivity().overridePendingTransition(R.transition.enter_forward, R.transition.exit_forward);
                Objects.requireNonNull(getActivity()).finish();
            });
        });

        handler = new Handler((msg) ->{
            if(adapter!=null) {
                Intent intent = new Intent("contactsLoading");
                intent.putExtra("loading", false);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
                adapter.notifyDataSetChanged();

            }
            return true;
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        if(getActivity().checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED && getActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            loadContacts();
        }else{
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(
                    permissionBroadCast, new IntentFilter("permissionReceived"));
            contactsView.setVisibility(GONE);
            permissionLayout.setVisibility(View.VISIBLE);
            permissionButton.setOnClickListener(v ->
                getActivity().requestPermissions(new String[] {
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE));
        }
    }
    private void loadContacts(){
        Intent intent = new Intent("contactsLoading");
        intent.putExtra("loading", true);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        fetchContactsInPhoneMemory();
    }
    private void mysqlGetConversationID(String firstPhoneNumber, String secondPhoneNumber, int position, OnMysqlFinish listener) {
        Thread thread = new Thread(() -> {
            URL url = null;
            HttpURLConnection connection = null;
            OutputStream outputStream = null;
            try {
                url = new URL("http://"+ip+"/ChatApp_war_exploded/request?option=get_conversation_id");
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestProperty("Accept-Charset", "UTF-8");
                connection.setRequestProperty("charset", "UTF-8");
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("first_phone_number", firstPhoneNumber);
                    jsonObject.put("second_phone_number", secondPhoneNumber);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                outputStream = connection.getOutputStream();
                outputStream.write(jsonObject.toString().getBytes());
                connection.connect();

                InputStream input = connection.getInputStream();
                byte[] buffer = new byte[4];
                int actuallyRead = input.read(buffer);
                int conversationID = -1;
                if(actuallyRead == buffer.length){
                    conversationID = ByteBuffer.wrap(buffer).getInt();
                }
                listener.onFinish(conversationID, position);

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
        });
        thread.start();
    }

    public void fetchContactsInPhoneMemory() {
       Thread thread = new Thread(() -> {
           String[] PROJECTION = new String[]{
                   ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                   ContactsContract.Contacts.DISPLAY_NAME,
                   ContactsContract.CommonDataKinds.Phone.NUMBER};
           if (getActivity() == null)
               return;
           ContentResolver cr = getActivity().getContentResolver();
           Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PROJECTION, null, null, ContactsContract.Contacts.DISPLAY_NAME + " ASC");
           if (cursor != null) {
               try {
                   final int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                   final int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                   String name, number;
                   while (cursor.moveToNext()) {
                       name = cursor.getString(nameIndex);
                       number = cursor.getString(numberIndex);
                       Contact contact = new Contact(number, name);
                       if (!contact.phoneNumber.equals("") && !contact.getPhoneNumber().equals(sender)) {
                           if (!contacts.contains(contact))
                               contacts.add(contact);
                       }
                   }
               } finally {
                   cursor.close();
               }
           }
           compareContacts();
       });
       thread.start();
    }

    private void compareContacts(){
            URL url = null;
            HttpURLConnection connection = null;
            OutputStream outputStream = null;
            try {
                url = new URL("http://"+ip+"/ChatApp_war_exploded/request?option=check_contacts_available");
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestProperty("Accept-Charset", "UTF-8");
                connection.setRequestProperty("charset", "UTF-8");
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                JSONObject jsonObject = new JSONObject();
                try {
                    JSONArray jsonArray = new JSONArray();
                    for (int i = 0; i < contacts.size(); i++) {
                        jsonArray.put(i, contacts.get(i).phoneNumber);
                    }
                    jsonObject.put("contacts", jsonArray);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                outputStream = connection.getOutputStream();
                outputStream.write(jsonObject.toString().getBytes());
                connection.connect();
                try {
                    InputStream input = connection.getInputStream();
                    byte[] buffer = new byte[1024];
                    StringBuilder stringBuilder = new StringBuilder();
                    int actuallyRead;
                    while ((actuallyRead = input.read(buffer)) != -1) {
                        stringBuilder.append(new String(buffer, 0, actuallyRead, "UTF-8"));
                    }
                    JSONObject jsonConversations = new JSONObject(stringBuilder.toString());
                    JSONArray jsonArray = jsonConversations.getJSONArray("contacts");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        Contact contact = new Contact();
                        contact.phoneNumber = jsonArray.getString(i);
                        contacts.remove(contact);
                    }

                    for (int i = 0; i < contacts.size() ; i++) {
                        contacts.get(i).setPicture(getBitmapFromURL("http://"+ip+"/ChatApp_war_exploded/request?option=ask_for_image&user="+ contacts.get(i).getPhoneNumber()));
                    }
                    if(handler != null){
                        handler.sendMessage(new Message());
                    }
                    Set<String> contactsSet = new HashSet<>();
                    for (int i = 0; i < contacts.size(); i++) {
                        contactsSet.add(contacts.get(i).toString());
                    }
                    if(getActivity() != null)
                        getActivity().getSharedPreferences(SettingsDialog.PREFS, Context.MODE_PRIVATE).edit().putStringSet(CONTACTS, contactsSet).apply();

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
    }
    private BroadcastReceiver permissionBroadCast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            if(intent.hasExtra("permission")){
                boolean permission = intent.getBooleanExtra("permission", false);
                if(permission) {
                    LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(permissionBroadCast);
                    permissionLayout.setVisibility(GONE);
                    contactsView.setVisibility(View.VISIBLE);
                    loadContacts();
                }
            }
        }

    };
    interface OnMysqlFinish{
        void onFinish(int conversationID, int position);
    }

}