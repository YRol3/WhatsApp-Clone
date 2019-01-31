import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.Statement;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MysqlHandler {


    public static final int GROUP_CONVERSATION = 2;
    public static final int PRIVATE_CONVERSATION = 1;

    public static int getUserID(Connection connection, String phoneNumber) throws SQLException {
        PreparedStatement statement = (PreparedStatement) connection.prepareStatement(" SELECT id FROM Users WHERE phone_number = ?");
        statement.setString(1, phoneNumber);
        ResultSet result =  statement.executeQuery();
        if(result.next())
            return result.getInt(1);
        else
            return -1;
    }
    public static String getUserPhoneNumber(Connection connection, int id) throws SQLException{
        PreparedStatement statement = (PreparedStatement) connection.prepareStatement(" SELECT phone_number FROM Users WHERE id = ?");
        statement.setInt(1, id);
        ResultSet result =  statement.executeQuery();
        if(result.next())
            return result.getString(1);
        else
            throw new SQLException("Could not find user with this phone number");
    }
    public static String encodeStringUrl(String url) {
        String encodedUrl =null;
        try {
            encodedUrl = URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return encodedUrl;
        }
        return encodedUrl;
    }

    public static String decodeStringUrl(String encodedUrl) {
        String decodedUrl =null;
        try {
            decodedUrl = URLDecoder.decode(encodedUrl, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return decodedUrl;
        }
        return decodedUrl;
    }
    public static void saveNewMessage(Connection connection, Message message) throws JSONException, SQLException{
        PreparedStatement statement = (PreparedStatement) connection.prepareStatement("INSERT INTO Messages(message_body, message_tag, user_id_sender, conversation_id, message_read) VALUES (?, ?, ?, ?, false)");
        message.setMessageBody(encodeStringUrl(message.getMessageBody()));
        statement.setString(1, message.getMessageBody());
        statement.setString(2, message.getMessageTag());
        statement.setInt(3, getUserID(connection, message.getSender()));
        statement.setInt(4, message.getConversationID());
        if(statement.executeUpdate() > 0){
            System.out.println("[MySQL] Successfully added new message to conversation id: " + message.getConversationID());
        }
    }
    public static List<String> getConversationParticipantsTokens(Connection connection, int conversationID, String filterByPhoneNumber) throws SQLException {
        List<Integer> participantsID = new ArrayList<>();
        PreparedStatement statement = (PreparedStatement) connection.prepareStatement(" SELECT user_id FROM Participants WHERE conversation_id = ?");
        statement.setInt(1, conversationID);
        ResultSet result = statement.executeQuery();
        while(result.next()){
            participantsID.add(result.getInt(1));
        }
        List<String> participantsToken = new ArrayList<>();
        for (int i = 0; i < participantsID.size(); i++) {
            statement = (PreparedStatement) connection.prepareStatement(" SELECT user_fcm_token FROM Users WHERE id = ? AND NOT phone_number = ?");
            statement.setInt(1, participantsID.get(i));
            statement.setString(2, filterByPhoneNumber);
            result = statement.executeQuery();
            if(result.next()){
                participantsToken.add(result.getString(1));
            }
        }
        return participantsToken;
    }

    public static void saveNewFCMToken(Connection connection, JSONObject jsonToken) throws JSONException, SQLException {
        String token = jsonToken.getString("token");
        String phoneNumber = jsonToken.getString("phone_number");
        PreparedStatement statement = (PreparedStatement) connection.prepareStatement("SELECT * FROM Users WHERE phone_number = ? LIMIT 1");
        statement.setString(1, phoneNumber);
        //
        if(statement.executeQuery().next())
            //User exists, just update
            statement = (PreparedStatement) connection.prepareStatement("UPDATE Users SET user_fcm_token = ? WHERE phone_number = ?");
        else
            //User does not exists, Insert new user with token
            statement = (PreparedStatement) connection.prepareStatement("INSERT INTO Users (user_fcm_token,  phone_number) VALUES (? , ?)");
        statement.setString(1, token);
        statement.setString(2, phoneNumber);
        if(statement.executeUpdate() > 0){
            System.out.println("[MySQL] Successfully updated token for " + phoneNumber);
        }
    }

    public static JSONObject retrieveConversationMessages(Connection connection, int converstaionID) throws SQLException {
        //id, message_body, message_tag, user_id_sender, conversation_id$
        List<Message> messages = new ArrayList<>();
        JSONObject jsonMessages = new JSONObject();
        PreparedStatement statement = (PreparedStatement) connection.prepareStatement(" SELECT * FROM Messages WHERE conversation_id = ?");
        statement.setInt(1, converstaionID);
        ResultSet result = statement.executeQuery();
        while(result.next()){
            Message message = new Message(decodeStringUrl(result.getString("message_body")), result.getString("message_tag"));
            message.setSender(getUserPhoneNumber(connection, result.getInt("user_id_sender")));
            message.setConversationID(result.getInt("conversation_id"));
            message.setMessagedRead(result.getBoolean("message_read"));
            messages.add(message);
        }
        JSONArray jsonArray = new JSONArray(messages);
        try {
            jsonMessages.put("messages", jsonArray);
            return jsonMessages;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] getUserPicture(Connection connection, int user_id){
           try {
               PreparedStatement statement = (PreparedStatement) connection.prepareStatement(" SELECT picture FROM Users WHERE id = ?");
               statement.setInt(1, user_id);
               ResultSet result = statement.executeQuery();
               if(result.next()) {
                   return result.getBytes(1);
               }
           } catch (SQLException e) {
               e.printStackTrace();
           }
        return null;
    }
    public static void uploadPicutre(Connection connection,byte[] picBuffer,  String phoneNumber){
        try {
            int user_id = getUserID(connection, phoneNumber);
            PreparedStatement statement = (PreparedStatement) connection.prepareStatement("UPDATE Users Set picture = ? WHERE id = ?");
            statement.setBytes(1, picBuffer);
            statement.setInt(2, user_id);
            if(statement.executeUpdate() > 0){
                System.out.println("[MySQL] Successfully updated picture for " + user_id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    public static void updateMessageRead(Connection connection, Message message) throws SQLException {
        PreparedStatement statement = (PreparedStatement) connection.prepareStatement("UPDATE Messages Set  message_read = ? WHERE message_tag = ? AND user_id_sender = ? AND message_body = ? AND conversation_id = ?");
        statement.setBoolean(1, message.isMessagedRead());
        statement.setString(2, message.getMessageTag());
        statement.setInt(3, getUserID(connection, message.getSender()));
        statement.setString(4, message.getMessageBody());
        statement.setInt(5, message.getConversationID());
        if(statement.executeUpdate() > 0){
            System.out.println("[MySQL] Successfully updated message read status in conversation: " + message.getConversationID());
        }
    }

    public static JSONObject pullConversations(Connection connection, String phoneNumber) throws SQLException, JSONException {
        List<Integer> conversationsID = new ArrayList<>();
        JSONObject jsonConversations = new JSONObject();
        int userID = getUserID(connection, phoneNumber);
        //Checking in which conversation the user participate
        PreparedStatement statement = (PreparedStatement) connection.prepareStatement(" SELECT * FROM Participants WHERE user_id = ?");
        statement.setInt(1, userID);
        ResultSet result = statement.executeQuery();
        while (result.next()) {
            conversationsID.add(result.getInt("conversation_id"));
        }
        List<Conversation> conversations = new ArrayList<>();
        for (int i = 0; i < conversationsID.size(); i++) {
            //check if such conversation exists in the Conversations table, and getting all the Conversations info
            statement = (PreparedStatement) connection.prepareStatement(" SELECT * FROM Conversations WHERE id = ?");
            int conversationID = conversationsID.get(i);
            statement.setInt(1, conversationID);
            result = statement.executeQuery();
            if (result.next()) {
                //Building the conversation object
                Conversation conversation = new Conversation(conversationsID.get(i));
                conversation.setConversationType(result.getInt("conversation_type"));
                if(conversation.getConversationType() == GROUP_CONVERSATION) {
                    //Group chat set title to the title of the conversation
                    conversation.setConversationTopic(result.getString("conversation_title"));
                }else{
                    //Private chat, sets the title to the second participant of the conversation
                    statement = (PreparedStatement) connection.prepareStatement("SELECT user_id FROM Participants WHERE conversation_id = ? AND NOT user_id = ? LIMIT 1");
                    statement.setInt(1, conversationID);
                    statement.setInt(2, userID);
                    ResultSet userIDResult = statement.executeQuery();
                    if(userIDResult.next()) {
                        String conversationTopic = getUserPhoneNumber(connection, userIDResult.getInt(1));
                        conversation.setConversationTopic(conversationTopic);
                    }
                }
                //Get last message of this conversation
                statement = (PreparedStatement) connection.prepareStatement("SELECT * FROM Messages WHERE conversation_id = ? ORDER BY id DESC LIMIT 1;");
                statement.setInt(1, conversationID);
                ResultSet lastMessageResult = statement.executeQuery();
                if(lastMessageResult.next()){
                    Message message = new Message(decodeStringUrl(lastMessageResult.getString("message_body")), lastMessageResult.getString("message_tag"));
                    message.setSender(lastMessageResult.getString("user_id_sender"));
                    message.setConversationID(conversationID);
                    message.setMessagedRead(lastMessageResult.getBoolean("message_read"));
                    conversation.setLastMessage(message);
                    conversations.add(conversation);
                }

            }
            JSONArray conversationsArray = new JSONArray(conversations);
            jsonConversations.put("conversations", conversationsArray);
        }
        return jsonConversations;
    }

    public static int getConversationID(Connection connection, String firstPhoneNumber, String secondPhoneNumber) throws SQLException {
        int firstUserID = getUserID(connection, firstPhoneNumber);
        int secondUserID = getUserID(connection, secondPhoneNumber);
        PreparedStatement statement = (PreparedStatement) connection.prepareStatement("SELECT u1.conversation_id " +
                "FROM Participants u1, Participants u2 " +
                "WHERE u1.user_id = ? " +
                "AND u2.user_id = ? " +
                "AND u1.conversation_id = u2.conversation_id");
        statement.setInt(1, firstUserID);
        statement.setInt(2, secondUserID);
        ResultSet result = statement.executeQuery();

        List<Integer> conversations = new ArrayList<>();

        while (result.next()) {
            conversations.add(result.getInt("conversation_id"));
        }

        for (int i = 0; i < conversations.size(); i++) {
            statement = (PreparedStatement) connection.prepareStatement("SELECT user_id FROM Participants WHERE conversation_id = ? ");
            statement.setInt(1, conversations.get(i));
            result = statement.executeQuery();
            int usersInConversations = 0;
            while(result.next()){
                usersInConversations++;
                if(usersInConversations > 2){
                    conversations.set(i, -1);
                    break;
                }
            }
            if(conversations.get(i) != -1)
                return conversations.get(i);
        }
        return -1;
    }

    @SuppressWarnings("Duplicates")
    public static int createNewConversationWithParticipants(Connection connection, String firstPhoneNumber, String secondPhoneNumber) throws SQLException {
        int firstUserID = getUserID(connection, firstPhoneNumber);
        int secondUserID = getUserID(connection, secondPhoneNumber);
        PreparedStatement statement = (PreparedStatement) connection.prepareStatement("INSERT INTO Conversations (conversation_type, total_participants) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
        statement.setInt(1, PRIVATE_CONVERSATION);
        statement.setInt(2, 2);
        if(statement.executeUpdate() > 0){
            ResultSet result = statement.getGeneratedKeys();
            if(result.next()){
                int conversationID = result.getInt(1);
                statement = (PreparedStatement) connection.prepareStatement("INSERT INTO Participants (user_id, conversation_id) VALUES (?, ?)");
                statement.setInt(1, firstUserID);
                statement.setInt(2, conversationID);
                if(statement.executeUpdate() == 0)
                    throw new SQLException("Could not insert data into Participants");
                statement = (PreparedStatement) connection.prepareStatement("INSERT INTO Participants (user_id, conversation_id) VALUES (?, ?)");
                statement.setInt(1, secondUserID);
                statement.setInt(2, conversationID);
                if(statement.executeUpdate() == 0)
                    throw new SQLException("Could not insert data into Participants");
                return conversationID;

            }
        }
        return -1;
    }
}
