import com.mysql.jdbc.PreparedStatement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {

    private static Connection conn;

    private DB(){

    }

    public static Connection getConn() throws SQLException {
        if(conn == null){
            String connString = "jdbc:mysql://localhost:3306/chat_db?characterEncoding=utf8";
            String userName = "root";
            String password = "08041996";
            DriverManager.registerDriver(new com.mysql.jdbc.Driver());
            conn = DriverManager.getConnection(connString, userName, password);
        }

        return conn;
    }


    public static void close(){
        if (conn != null) {
            try {
                conn.close();
                conn = null;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void init(Connection connection) throws SQLException {
        if (connection != null) {
            PreparedStatement statement = null;
            try {
                //Create table Users if not exists
                statement = (PreparedStatement) connection.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS Users(id int NOT NULL AUTO_INCREMENT, phone_number text, user_fcm_token text, picture BLOB, PRIMARY KEY (id))");

                statement.executeUpdate();

                //Create table Participants if not exists
                statement = (PreparedStatement) connection.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS Participants(id int NOT NULL AUTO_INCREMENT, user_id int, conversation_id int, PRIMARY KEY (id))");

                statement.executeUpdate();

                //Create table Conversations if not exists
                statement = (PreparedStatement) connection.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS Conversations(id int NOT NULL AUTO_INCREMENT, conversation_type int, conversation_title VARCHAR(30), total_participants int, total_messages int, PRIMARY KEY (id))");
                statement.executeUpdate();

                //Create table Messages if not exists
                statement = (PreparedStatement) connection.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS Messages(id int NOT NULL AUTO_INCREMENT, message_body text, message_tag text, user_id_sender int, conversation_id int, message_read boolean, PRIMARY KEY (id))");
                statement.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }


        }
    }

}