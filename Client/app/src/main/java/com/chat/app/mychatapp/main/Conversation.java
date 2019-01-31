package com.chat.app.mychatapp.main;

import android.graphics.Bitmap;

import com.chat.app.mychatapp.Chat.Message;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;

public class Conversation implements Serializable {

    private  Message lastMessage;
    private int uid;
    private transient Bitmap conversationPic;
    private String conversationTopic;
    private int conversationType;
    public Conversation(int uid) {
        this.uid = uid;
    }

    public Conversation(JSONObject conversation) throws JSONException {
        lastMessage = new Message(conversation.getJSONObject("lastMessage"));
        conversationTopic = conversation.getString("conversationTopic");
        conversationType = conversation.getInt("conversationType");
        uid = lastMessage.getConversationID();
    }

    public int getUid() {
        return uid;
    }


    public void setLastMessage(Message lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setConversationPic(Bitmap conversationPic) {
        this.conversationPic = conversationPic;
    }

    public void setConversationTopic(String conversationTopic) {
        this.conversationTopic = conversationTopic;
    }

    public int getConversationType() {
        return conversationType;
    }


    public String getConversationTag(){
        return lastMessage.getMessageTag();
    }

    public Bitmap getConversationPic() {
        return conversationPic;
    }

    public String getConversationTopic() {
        return conversationTopic;
    }

    public String getConversationBodySnippet(int len){
        if(len >= lastMessage.getMessageBody().length()) return lastMessage.getMessageBody();
        return lastMessage.getMessageBody().substring(0, len-3) + "...";
    }
}
