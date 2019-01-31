import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import java.io.*;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;


public class mainServlet extends javax.servlet.http.HttpServlet {
    public static final String COMMAND = "option";

    //change value to true or false if you need tables to be generated
    public static final boolean CREATE_MYSQL_TABLES = false;
    private Connection connection;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            connection = DB.getConn();
            if(CREATE_MYSQL_TABLES)
                DB.init(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws javax.servlet.ServletException, IOException {
        System.out.println("in Do Post");
        InputStream inputStream = null;
        BufferedReader in = null;
        request.setCharacterEncoding("UTF-8");
        try {
            inputStream = request.getInputStream();
            in = new BufferedReader(new InputStreamReader(inputStream, UTF_8));
                char[] buffer = new char[1024];
                StringBuilder requestMessage = new StringBuilder();
                int actuallyRead;
                while ((actuallyRead = in.read(buffer)) != -1) {
                    requestMessage.append(new String(buffer, 0, actuallyRead));
                }
                if (request.getParameter(COMMAND).equals("new_message")) {
                    JSONObject jsonObject = null;
                    OutputStream outputStream = null;
                    try {
                        System.out.println("in new message");
                        jsonObject = new JSONObject(requestMessage.toString());
                        Message message = new Message(jsonObject.getJSONObject("message"));
                        if(message.getConversationID() == -1) {
                            String secondPhoneNumber = jsonObject.getString("secondNumber");
                            message.setConversationID(MysqlHandler.createNewConversationWithParticipants(connection, message.getSender(), secondPhoneNumber));
                            outputStream = response.getOutputStream();
                            byte[] intBuffer = new byte[4];
                            ByteBuffer.wrap(intBuffer).putInt(message.getConversationID());
                            outputStream.write(intBuffer);
                        }
                        FCMHandler.sendNewMessage(connection, message);
                        MysqlHandler.saveNewMessage(connection, message);
                        System.out.println("message saved");
                    } catch (Exception e) {

                    }
                }
                else if(request.getParameter(COMMAND).equals("upload_user_profile_pic")){
                    System.out.println("In upload user profile picture");
                    JSONObject jsonObject = null;
                    try{
                        jsonObject = new JSONObject(requestMessage.toString());
                        String phoneNumber  = jsonObject.getString("phone_number");
                        String pic = jsonObject.getString("user_pic");
                        pic = pic.replace("\\","");
                        pic = pic.replace("\n", "");
                        byte[] picByteArray = Base64.getDecoder().decode(pic);
                        MysqlHandler.uploadPicutre(connection,  picByteArray, phoneNumber);
                        System.out.println("Updated profile picture for user " + phoneNumber);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else if(request.getParameter(COMMAND).equals("get_conversation_id")){
                    JSONObject jsonObject = null;
                    OutputStream outputStream = null;
                    try{
                        System.out.println("In get conversation id");
                        jsonObject = new JSONObject(requestMessage.toString());
                        String firstPhoneNumber = jsonObject.getString("first_phone_number");
                        String secondPhoneNumber = jsonObject.getString("second_phone_number");
                        int conversationID = MysqlHandler.getConversationID(connection, firstPhoneNumber, secondPhoneNumber);
                        outputStream = response.getOutputStream();
                        byte[] intBuffer = new byte[4];
                        ByteBuffer.wrap(intBuffer).putInt(conversationID);
                        outputStream.write(intBuffer);
                        System.out.println("sent successfully conversation id = " + conversationID);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                else if(request.getParameter(COMMAND).equals("check_contacts_available")){
                    JSONObject jsonContacts = null;
                    OutputStream outputStream = null;
                    try{
                        System.out.println("In Available contacts check");
                        jsonContacts = new JSONObject(requestMessage.toString());
                        JSONArray jsonArray = jsonContacts.getJSONArray("contacts");
                        List<String> phoneNumbers = new ArrayList<>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            String phoneNumber = jsonArray.getString(i);
                            if(MysqlHandler.getUserID(connection, phoneNumber) == -1){
                                phoneNumbers.add(phoneNumber);
                            }
                        }
                        jsonArray = new JSONArray(phoneNumbers);
                        jsonContacts = new JSONObject();
                        jsonContacts.put("contacts", jsonArray);
                        outputStream = response.getOutputStream();
                        outputStream.write(jsonContacts.toString().getBytes());
                        System.out.println("sent succesfuly");

                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                else if(request.getParameter(COMMAND).equals("pull_conversations")) {
                    System.out.println("in pull conversations");
                    JSONObject jsonObject = null;
                    OutputStream outputStream = null;
                    try {
                        jsonObject = new JSONObject(requestMessage.toString());
                        String phoneNumber = jsonObject.getString("phone_number");
                        jsonObject = MysqlHandler.pullConversations(connection, phoneNumber);
                        System.out.println("Json built succesfuly");
                        if (jsonObject != null) {
                            outputStream = response.getOutputStream();
                            outputStream.write(jsonObject.toString().getBytes());
                            System.out.println("sent succesfuly");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                else if(request.getParameter(COMMAND).equals("pull_conversation_messages")){
                    System.out.println("In messages pull request");
                    OutputStream outputStream = null;
                    try {
                        JSONObject jsonObject = new JSONObject(requestMessage.toString());
                        int conversationID = jsonObject.getInt("conversation_id");
                        System.out.println("coversation id " + conversationID);
                        JSONObject jsonAnswer = MysqlHandler.retrieveConversationMessages(connection, conversationID);
                        if (jsonAnswer != null) {
                            System.out.println(jsonAnswer);
                            outputStream = response.getOutputStream();
                            outputStream.write(jsonAnswer.toString().getBytes());
                            System.out.println("send succesfuly");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }finally {
                        if (outputStream != null) {
                            outputStream.close();
                        }
                    }

                }
                else if(request.getParameter(COMMAND).equals("new_token")){
                    System.out.println("in new token received to the server");
                    try{
                        JSONObject jsonObject = new JSONObject(requestMessage.toString());
                        MysqlHandler.saveNewFCMToken(connection, jsonObject);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                //TODO - Implement token based image syncing (sending to the client)
                else if(request.getParameter(COMMAND).equals("ask_for_image")){
                    System.out.println("in ask for image");
                    String user_phone_number = request.getParameter("user");
                    try {
                        int user_id = MysqlHandler.getUserID(connection, user_phone_number);
                        OutputStream outputStream = response.getOutputStream();
                        byte[] pictureBuffer = MysqlHandler.getUserPicture(connection, user_id);
                        if(pictureBuffer != null) {
                            outputStream.write(pictureBuffer);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    protected void doGet(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws javax.servlet.ServletException, IOException {
    }

    @Override
    public void destroy() {
        DB.close();
    }
}
