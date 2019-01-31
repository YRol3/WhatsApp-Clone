package com.chat.app.mychatapp.Chat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class Message implements Serializable {

    private String messageBody;
    private String messageTag;
    private String sender;
    private boolean receiver;
    private boolean messagedRead;
    private int conversationID;
    

    public Message(String messageBody, String messageTag,int conversationID ,boolean reciver) {
        this.messageBody = messageBody;
        this.messageTag = messageTag;
        this.receiver = reciver;
        this.conversationID = conversationID;
    }
    public Message(JSONObject jsonObject) throws JSONException {
        messageBody = jsonObject.getString("messageBody");
        messageTag = jsonObject.getString("messageTag");
        messagedRead = jsonObject.getBoolean("messagedRead");
        sender = jsonObject.getString("sender");
        conversationID = jsonObject.getInt("conversationID");
    }
    public void setMessagedRead(boolean messagedRead) {
        this.messagedRead = messagedRead;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public boolean isReceiver() {
        return receiver;
    }

    public String getMessageTag() {
        return messageTag;
    }

    public void setMessageTag(String messageTag) {
        this.messageTag = messageTag;
    }

    public boolean isMessagedRead() {
        return messagedRead;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSender() {
        return sender;
    }

    public void setReceiver(boolean receiver) {
        this.receiver = receiver;
    }

    public int getConversationID() {
        return conversationID;
    }

    public JSONObject toJSON() {
        JSONObject messageObject = null;
        try {
            messageObject = new JSONObject();
            messageObject.put("messageBody", this.messageBody);
            messageObject.put("messagedRead", this.messagedRead);
            messageObject.put("messageTag", this.messageTag);
            messageObject.put("sender", this.sender);
            messageObject.put("conversationID", this.conversationID);
            return messageObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }


}
