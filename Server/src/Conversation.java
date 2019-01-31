
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class Conversation {

    private Message lastMessage;
    private int uid;
    private byte[] conversationPic;
    private String conversationTopic;
    private int conversationType;
    public Conversation(int uid) {
        this.uid = uid;
    }

    public JSONObject toJSON() {
        JSONObject Conversation = null;
        try {
            Conversation = new JSONObject();
            JSONArray messages = new JSONArray(lastMessage);
            Conversation.put("Messages", messages);
            Conversation.put("uid", uid);
            return Conversation;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public int getUid() {
        return uid;
    }

    public Message getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(Message lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public void setConversationPic(byte[] conversationPic) {
        this.conversationPic = conversationPic;
    }

    public void setConversationTopic(String conversationTopic) {
        this.conversationTopic = conversationTopic;
    }

    public int getConversationType() {
        return conversationType;
    }

    public void setConversationType(int conversationType) {
        this.conversationType = conversationType;
    }

    public String getConversationTag(){
        return lastMessage.getMessageTag();
    }

    public byte[] getConversationPic() {
        return conversationPic;
    }

    public String getConversationTopic() {
        return conversationTopic;
    }
}
