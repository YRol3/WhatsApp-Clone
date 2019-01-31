
import org.json.JSONException;
import org.json.JSONObject;

public class Message {

    private String messageBody;
    private String messageTag;
    private String sender;
    private boolean messagedRead;
    private int conversationID;

    public Message(String messageBody, String messageTag) {
        this.messageBody = messageBody;
        this.messageTag = messageTag;
    }

    public Message(JSONObject jsonObject) throws JSONException {
        messageBody = jsonObject.getString("messageBody");
        messageTag = jsonObject.getString("messageTag");
        messagedRead = jsonObject.getBoolean("messagedRead");
        sender = jsonObject.getString("sender");
        conversationID = jsonObject.getInt("conversationID");


    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public void setMessagedRead(boolean messagedRead) {
        this.messagedRead = messagedRead;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public String getMessageTag() {
        return messageTag;
    }

    public void setMessageTag(String messageTag) {
        this.messageTag = messageTag;
    }

    public int getConversationID() {
        return conversationID;
    }

    public void setConversationID(int conversationID) {
        this.conversationID = conversationID;
    }

    public boolean isMessagedRead() {
        return messagedRead;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
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
