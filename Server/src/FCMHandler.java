import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class FCMHandler  {
    //fcm legacy key
    public static final String fcm_key = "AIzaSyA4f0Z_hzbTuEdEVLnCMRYzNDwQA-DV1Us";
    public static void sendNewMessage(Connection connection, Message message){
        System.out.println("in send message");
        List<String> participants = null;
        try {
            System.out.println("sender phone number + " + message.getSender());
            participants = MysqlHandler.getConversationParticipantsTokens(connection, message.getConversationID(), message.getSender());
            for(String token: participants){
                sendFCM(message.toJSON(), token);
                System.out.println("[FCM] sent message to " + token);
           }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private static void sendFCM(JSONObject message, String to){
        URL url = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        HttpURLConnection connection = null;
        try{
            url = new URL("https://fcm.googleapis.com/fcm/send");
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization","key="+fcm_key);
            connection.connect();
            JSONObject jsonMessage = new JSONObject();
            JSONObject jsonData = new JSONObject();
            try {
                jsonMessage.put("to", to);
                jsonData.put("message", message);
                jsonMessage.put("data", jsonData);
                jsonMessage.put("priority", "high");


            } catch (JSONException e) {
                e.printStackTrace();
            }
            outputStream = connection.getOutputStream();
            outputStream.write(jsonMessage.toString().getBytes());
            inputStream = connection.getInputStream();
            StringBuilder stringBuilder = new StringBuilder();
            byte[] buffer = new byte[1024];
            int actuallyRead;
            while ((actuallyRead = inputStream.read(buffer)) != -1){
                stringBuilder.append(new String(buffer, 0, actuallyRead));
            }
            System.out.println(stringBuilder.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
}
