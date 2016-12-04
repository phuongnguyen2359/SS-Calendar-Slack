/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.UUID;
import javax.websocket.Session;
import java.util.Arrays;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author USER
 */
public class MessageManager {

    Session session;
    ObjectMapper mapper;
    HttpURLConnection connection;
    ArrayList<String[]> userConfirmation = new ArrayList();
    String token;
    String bot_id;

    public MessageManager(HttpURLConnection connection,
            Session session, ObjectMapper mapper, String token, String bot_id) {
        this.session = session;
        this.mapper = mapper;
        this.connection = connection;
        this.token = token;
        this.bot_id = bot_id;
    }

    public void handleMessage(JsonNode node){
        String user = node.findPath("user").asText();
        String type = node.findPath("type").asText();
        if (user.equals(bot_id) || !type.equals("message")) {
            return;
        }
        String channel = node.findPath("channel").asText();
        String request = node.findPath("text").asText();
        System.out.println("TESTTTTT:" + userConfirmation.size());
        if (request.toLowerCase().contains("calendar")) {
            userConfirmation.remove(findRequest(user));
            showCalendar(channel, request);
        } else if (request.toLowerCase().contains("booking")) {
            userConfirmation.remove(findRequest(user));
            showBookings(channel, user);
        } else if (request.toLowerCase().contains("help")) {
            userConfirmation.remove(findRequest(user));
            showHelp(channel);
        } else if (request.toLowerCase().contains("book")) {

            try {
                userConfirmation.remove(findRequest(user));
                bookRoomPrepare(user, channel, request);
            } catch (IOException ex) {
                Logger.getLogger(MessageManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if(findRequest(user) != null && findRequest(user)[0].equals("book") 
                && Integer.parseInt(findRequest(user)[7]) < 5){
            try {
                bookRoomPrepare(user, channel, request);
            } catch (IOException ex) {
                Logger.getLogger(MessageManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        } else if (request.toLowerCase().contains("cancel")) {

            userConfirmation.remove(findRequest(user));
            cancelRoomPrepare(user, channel, request);
        } else if (request.toLowerCase().contains("confirm")) {

            userConfirmation.remove(findRequest(user));
            confirmRoom(user, channel, request);
        }else if (request.toLowerCase().contains("yes")) {
            String[] tmp = findRequest(user);
            if (tmp != null) {
                if (tmp[0].equals("book") 
                        && findRequest(user)[7].equals("5")) {
                    bookRoom(channel, tmp);
                } else if (tmp[0].equalsIgnoreCase("cancel")) {
                    cancelRoom(channel, tmp);
                }
            }
        } else if (request.toLowerCase().contains("no")) {
            userConfirmation.remove(findRequest(user));
            sendMessage(channel, "Your request is canceled!");
        } else {
            userConfirmation.remove(findRequest(user));
            sendMessage(channel, "Enter 'help' to read more information");
        }
    }

    public void showCalendar(String channel, String request) {
        try {          
            String[] keywords = request.split(" ");
            int pos = 0;
            for(int i = 0; i < keywords.length; i++){
                if(keywords[i].toLowerCase().contains("calendar")){
                    pos = i;
                }
            }
            String date = keywords[pos+1];
            if (!date.trim().equalsIgnoreCase("today")
                    && !date.trim().equalsIgnoreCase("tmr")) {
                if (date.length() != 5) {
                    sendMessage(channel, "You need to enter date as dd/mm");
                    return;
                }

                if (date.contains("/") && date.split("/").length != 2) {
                    sendMessage(channel, "You need to enter date as dd/mm");
                    return;
                } else if (date.contains("-") && date.split("-").length != 2) {
                    sendMessage(channel, "You need to enter date as dd/mm");
                    return;
                }
            }

            //-------HTTP Connection
            connection = (HttpURLConnection) connection.getURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            
            //------- DATABASE CONNECTION
            Connection dbConnection = getConnection();
            
            final DataOutputStream dos = new DataOutputStream(
                    connection.getOutputStream());
            
            //-------GET NUM OF ROOMs
            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(id) FROM room WHERE status = 1");
            int roomCount = 0;
            while (rs.next()) {
                roomCount = rs.getInt(1);
            }
            String[][] room = new String[roomCount][2];
            
            //-------GET ROOM INFO
            rs = stmt.executeQuery("SELECT * FROM room WHERE status = 1");
            int i = 0;
            while (rs.next()) {
                room[i][0] = rs.getString("id");
                room[i][1] = rs.getString("name");
                i++;
            }

            //-------CURRENT DATETIME ON HEROKU
            rs = stmt.executeQuery("SELECT CONVERT_TZ(now(),'+00:00','+07:00')");
            String curDateTime = "";
            while (rs.next()) {
                curDateTime += rs.getString(1);
            }
            
          
            //--current month
            String month = "";
            rs = stmt.executeQuery("SELECT MONTH(CURDATE()) as month");
            while (rs.next()) {
                month = rs.getString("month");
            }
            
            //--current year
            String year = "";
            rs = stmt.executeQuery("SELECT YEAR(CURDATE()) as year");
            while (rs.next()) {
                year = rs.getString("year");
            }

            
            //--user-date
            String userDay = "";
            String userMonth = "";
                                 
            
                     
            if (!date.trim().equalsIgnoreCase("today")
                    && !date.trim().equalsIgnoreCase("tmr")) {
                if (date.contains("/")) {
                    userDay = date.split("/")[0].trim();
                    userMonth = date.split("/")[1].trim();
                } else if (date.contains("-")) {
                    userDay = date.split("-")[0].trim();
                    userMonth = date.split("-")[1].trim();
                }
                if (Integer.parseInt(month) == 12 && Integer.parseInt(userMonth) < 12) {
                    year = "" + (Integer.parseInt(year) + 1);
                }
                date = year + "/" + userMonth + "/" + userDay;
            }
            
            
            
            //today and tmr
            if(date.trim().equalsIgnoreCase("today")){
                rs = stmt.executeQuery("SELECT CURDATE() as date");
                while(rs.next()){
                    date = rs.getString("date");
                }
            } else if(date.trim().equalsIgnoreCase("tmr")){
                rs = stmt.executeQuery("SELECT DATE_ADD(CURDATE(), "
                        + "INTERVAL 1 DAY) as date");
                while(rs.next()){
                    date = rs.getString("date");
                }
            }
            System.out.println(date);
            if(!isDateValid(date, "yyyy/mm/dd")){
                sendMessage(channel, "Invalid date!");
                userConfirmation.remove(request);
                return;
            }
            
            JSONObject field;
            JSONObject attach;
            JSONArray fieldArray;
            JSONArray atmArray = new JSONArray();
            //-------CREATE ATTACHMENT
            for (int x = 0; x < roomCount; x++) {
                attach = new JSONObject();
                fieldArray = new JSONArray();
                boolean flag = false;
                rs = stmt.executeQuery("SELECT * FROM booking WHERE room_id = "
                        + room[x][0] + " AND start_time BETWEEN '" + date + " 00:00:00' AND '" + date + " 23:59:59'"
                        + "AND book_status = 1 ORDER BY start_time ASC");
                while (rs.next()) {
                    field = new JSONObject();
                    String start = rs.getString("start_time").split(" ")[1];
                    String end = rs.getString("end_time").split(" ")[1];
                    String purpose = rs.getString("purpose");                  
  
                    field.put("value", start.split(":")[0] + ":" + start.split(":")[1]
                            + " - " + end.split(":")[0] + ":" + end.split(":")[1]
                            + "\t\t--->\t\t" + purpose
                    );
                    field.put("short", false);
                    fieldArray.put(field);
                    if (compareTime(curDateTime.split(" ")[1], rs.getString("start_time").split(" ")[1]) <= 1
                            && compareTime(rs.getString("end_time").split(" ")[1], curDateTime.split(" ")[1]) <= 1) {
                        flag = true;
                    }
                }
                
                attach.put("title", room[x][1]);
                attach.put("fields", fieldArray);
                if (flag) {
                    attach.put("color", "danger");
                } else {
                    attach.put("color", "good");
                }
                atmArray.put(attach);
            }
            
            System.out.println(roomCount);
            System.out.println("test: " + atmArray);
            /*for(int x = 0; x < room.length; x++){
            System.out.println("Room Info: " + room[x][0] + " - " + room[x][1]);
            }*/
            
            String attachments = URLEncoder.encode(atmArray.toString(), "UTF-8");
            
            String text = (URLEncoder.encode("*" + date + " Timetable*\n_The session listed below are already "
                    + "booked._\n\t_+ 1 session = 30 minutes_\n\t_+ Green = Free "
                    + "now_\n\t_+ Red = Used now_", "UTF-8"));
            
            String prepare = "token=" + token + "&channel=" + channel + "&text=" + text
                    + "&attachments=" + attachments + "&as_user=true&pretty=1";
            
            dos.writeBytes(prepare);
            dos.flush();
            dos.close();
            stmt.close();
            dbConnection.close();
            final InputStream is = connection.getInputStream();
            is.close();
        } catch (IOException | URISyntaxException | JSONException ex) {
            Logger.getLogger(MessageManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            sendMessage(channel, "Please check your date! (yyyy/mm/dd)");
        }
    }

    public void showHelp(String channel) {
        try {
            connection = (HttpURLConnection) connection.getURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            final DataOutputStream dos = new DataOutputStream(
                    connection.getOutputStream());
            
            JSONObject field1 = new JSONObject();
            JSONObject field2 = new JSONObject();
            JSONObject field3 = new JSONObject();
            JSONObject field4 = new JSONObject();
            
            JSONObject attach1 = new JSONObject();
            
            JSONArray fieldArray = new JSONArray();
            JSONArray atmArray = new JSONArray();
            
            field1.put("title", "Show calendar");
            field1.put("value", "To show calendar of day X, Enter 'calendar X' "
                    + "X should be in dd/mm format."
                    + "\nExample 1: calendar today\nExample 2: calendar 03/09");
            field1.put("short", false);
            
            field2.put("title", "Book");
            field2.put("value", "Needed the word 'book room', Room name, Start time, Duration"
                    + " and Booking Purpose(Optional)\nExample: I want to book room "
                    + "Room 1");
            field2.put("short", false);
            
            field3.put("title", "Cancel");
            field3.put("value", "Needed the word 'cancel' and request code. A request "
                    + "code is the code that sscalendar_bot send when you successfully "
                    + "book a room. If you forget your code, use Show my bookings syntax "
                    + "\nExample: cancel R23A45D7");
            field3.put("short", false);
            
            field4.put("title", "Show my bookings");
            field4.put("value", "To show all of your bookings, enter 'my bookings'.\n"
                    + "Example 1: please show my bookings\nExample 2: I want to see my bookings");
            field4.put("short", false);
            
            fieldArray.put(field1);
            fieldArray.put(field2);
            fieldArray.put(field3);
            fieldArray.put(field4);
            
            attach1.put("fields", fieldArray);
            atmArray.put(attach1);
            
            String attachments = URLEncoder.encode(atmArray.toString(), "UTF-8");
            
            String prepare = "token=" + token + "&channel=" + channel + "&text=*Syntax%20guideline*&attachme"
                    + "nts=" + attachments + "&as_user=true&pretty=1";
            dos.writeBytes(prepare);
            dos.flush();
            dos.close();
            
            final InputStream is = connection.getInputStream();
            is.close();
        } catch (ProtocolException ex) {
            Logger.getLogger(MessageManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | JSONException ex) {
            Logger.getLogger(MessageManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendMessage(String channel, String msg) {
        try {
            ObjectNode msgJson = mapper.createObjectNode();
            msgJson.put("type", "message");
            msgJson.put("text", msg);
            msgJson.put("channel", channel);
            session.getBasicRemote().sendText(msgJson.toString());
        } catch (IOException ex) {
            Logger.getLogger(MessageManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    

    public void changeSession(Session session) {
        this.session = session;
    }

    public void bookRoomPrepare1(String user, String channel, String request) throws IOException {
        String[] keywords = request.split(" ");
        String[] params = {"book", user, "", "", "", "", "", "0"};//{"book",user,room,start,dur,purpose,date,"0"}
        int[] keywordIndx = new int[6];
        keywordIndx[5] = keywords.length;
        int indxCommand = 0;

        //find room, duration and purpose
        for (int i = 0; i < keywords.length; i++) {
            if (keywords[i].trim().equalsIgnoreCase("book")) {
                indxCommand = i;
                break;
            }
        }

        int flag = 0;
        ArrayList<String> tmp = new ArrayList<>();
        for (int i = indxCommand + 1; i < keywords.length; i++) {
            if ((keywords[i].trim().equalsIgnoreCase("room")
                    || keywords[i].trim().equalsIgnoreCase("at")
                    || keywords[i].trim().equalsIgnoreCase("for")
                    || keywords[i].trim().equalsIgnoreCase("to")
                    || keywords[i].trim().equalsIgnoreCase("date"))
                    && !tmp.contains(keywords[i].trim().toLowerCase())) {
                keywordIndx[flag] = i;
                tmp.add(keywords[i].trim().toLowerCase());
                flag++;
            }

            if (flag > 5) {
                break;
            }
        }

        //not enough keywords
        if (flag < 5) {
            sendMessage(channel, "Not enough information!");
            return;
        }

        Arrays.sort(keywordIndx);

        for (int i = 0; i < keywordIndx.length - 1; i++) {
            for (int j = keywordIndx[i] + 1; j < keywordIndx[i + 1]; j++) {
                switch (keywords[keywordIndx[i]].trim()) {
                    case "room":
                        params[2] += keywords[j] + " ";
                        break;
                    case "at":
                        params[3] += keywords[j];
                        break;
                    case "for":
                        params[4] += keywords[j];
                        break;
                    case "to":
                        params[5] += keywords[j] + " ";
                        break;
                    case "date":
                        params[6] += keywords[j];
                        break;
                }
            }
        }
        
        
        for(int i = 2; i < 7; i++){
            params[i] = params[i].trim();
        }
                         
        //not enough info
        for(int i = 2; i < 7; i++){//room,start,dur,purpose,date
            if(params[i].equals("")){
                if(i == 2){
                    sendMessage(channel, "Missing room");
                }else if( i == 3){
                    sendMessage(channel, "Missing start time");
                }else if( i == 4){
                    sendMessage(channel, "Missing end time");
                }else if( i == 5){
                    sendMessage(channel, "Missing booking purpose");
                }else if( i == 6){
                    sendMessage(channel, "Missing date");
                }
                
                return;
            }
        }     
        //check start time
        params[3] = simplifyTime(params[3]);
        System.out.println(params[3]);
        System.out.println(timeToHours(params[3]));
        if(params[3].equals("-1")){
            sendMessage(channel, "You can only book from 9AM to 6PM");
            return;
        }       
        else if(Double.parseDouble(timeToHours(params[3]))%0.5 != 0){
            sendMessage(channel, "Please check your time (e.g 9:00, 9:30, 10:00)");
            return;
        }
        
         
        //check date
        if (!params[6].trim().equalsIgnoreCase("today") 
                && !params[6].trim().equalsIgnoreCase("tmr")) {
            if (params[6].length() != 10) {
                sendMessage(channel, "You need to enter date as yyyy/mm/dd");
                return;
            }

            if (params[6].contains("/") && params[6].split("/").length != 3) {
                sendMessage(channel, "You need to enter date as yyyy/mm/dd");
                return;
            } else if (params[6].contains("-") && params[6].split("-").length != 3) {
                sendMessage(channel, "You need to enter date as yyyy/mm/dd");
                return;
            }
        }
           
        
        
        //check duration
        if(!params[4].toLowerCase().contains("hour")){
            sendMessage(channel, "Please check your duration! (e.g 0,5hour, 1hour)");
            return;
        }
        params[4] = params[4].split("hour")[0];
        if(Double.parseDouble(params[4])%0.5 != 0){
            sendMessage(channel, "Please check your duration! (eg. 0,5hour, 1hour)");
            return;
        }
       
        
        //change duration into end_time
        params[4] = addTime(params[3], params[4]);
        if(compareTime(params[4], "18:00") == 1){
            sendMessage(channel, "You can only book from 9AM to 6PM");
            return;
        }      
        userConfirmation.add(params);

        sendMessage(channel, "Booking info:"
                + "\n\t<> Date: " + params[6]
                + "\n\t<> Room: " + params[2]
                + "\n\t<> Start Time: " + params[3]
                + "\n\t<> End Time: " + params[4]
                + "\n\t<> Purpose: " + params[5]
                + "\nIs it correct? (Yes/No)");
    }

    public void cancelRoomPrepare(String user, String channel, String request) {
        try {
            Connection dbConnection = getConnection();
            Statement stmt = dbConnection.createStatement();
            
            String[] keywords = request.split(" ");
            String[] params = {"cancel", user, "", "0"}; // {"cancel", user, code, "0"}
            int indxCommand = 0;
            for (int i = 0; i < keywords.length; i++) {
                if (keywords[i].trim().equalsIgnoreCase("cancel")) {
                    indxCommand = i;
                    break;
                }
            }
            
            //retrieve request code
            for (int i = indxCommand + 1; i < keywords.length; i++) {
                if (keywords[i].startsWith("R") && keywords[i].length() == 8) {
                    params[2] = keywords[i];
                    break;
                }
            }
            
            //not enough info
            if (params[2].equals("")) {
                sendMessage(channel, "You entered invalid code! Please check"
                        + " your code using 'show booking' function");
                return;
            }
            
           
            
            //send confirmation to user
            ResultSet rs = stmt.executeQuery("SELECT * FROM booking, room WHERE "
                    + "booking.room_id = room.id AND "
                    + "request_code = '" + params[2] + "'"
                    + " AND booker_id = '" + params[1] + "'"
                    + " AND book_status = 1");
            if(!rs.first()){
                sendMessage(channel, "Sorry, I cannot found your code. Please check"
                        + " your code using 'show booking' function");
                stmt.close();
                dbConnection.close();
                return;
            }
            
            userConfirmation.add(params);
            sendMessage(channel, "Cancel Booking info:"
                    + "\n\t<> Room: " + rs.getString("room.name")
                    + "\n\t<> Start Time: " + rs.getString("start_time").split(" ")[1].substring(0, 5)
                    + "\n\t<> End Time: " + rs.getString("end_time").split(" ")[1].substring(0, 5)
                    + "\n\t<> Purpose: " + rs.getString("purpose")
                    + "\nIs it correct? (Yes/No)");

            
            stmt.close();
            dbConnection.close();
        } catch (URISyntaxException | SQLException ex) {
            Logger.getLogger(MessageManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void bookRoom(String channel, String[] request){
        
        HttpURLConnection connectionInfo = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            final URL urlChat = new URL("https://slack.com/api/users.info");
            connectionInfo = (HttpURLConnection) urlChat.openConnection();
            connectionInfo.setRequestMethod("POST");
            connectionInfo.setConnectTimeout(5000);
            connectionInfo.setUseCaches(false);
            connectionInfo.setDoInput(true);
            connectionInfo.setDoOutput(true);
                    
            final String payload = "token=" + token + "&user="+ request[1] + "&pretty=1";
            // Send request
            final DataOutputStream wrRTM = new DataOutputStream(
                    connectionInfo.getOutputStream());
         
            
            wrRTM.writeBytes(payload);
            wrRTM.flush();
            wrRTM.close();
                  
            // Get Response
            final InputStream isRTM = connectionInfo.getInputStream();   
            JsonNode response = mapper.readTree(isRTM);
            isRTM.close();
 
            String username = response.get("user").findPath("name").asText();
            
            //------- DATABASE CONNECTION
            Connection dbConnection = getConnection();
            Statement stmt = dbConnection.createStatement();
            
            String code = generateCode();
            
            ResultSet rs = stmt.executeQuery("SELECT id FROM room WHERE name ='"
                    + request[2] + "' AND status = 1");
            
            String room_id;
            
            if(rs.first()){
                room_id = rs.getString("id");
            }
            else{
                sendMessage(channel, "I cannot find your room!");
                userConfirmation.remove(request);
                return;
            }
           
            //--current month
            String month = "";
            rs = stmt.executeQuery("SELECT MONTH(CURDATE()) as month");
            while (rs.next()) {
                month = rs.getString("month");
            }
            
            //--current year
            String year = "";
            rs = stmt.executeQuery("SELECT YEAR(CURDATE()) as year");
            while (rs.next()) {
                year = rs.getString("year");
            }

            
            //--user-date
            String userDay = "";
            String userMonth = "";
                               
            String booker_id = request[1];
            String date = request[3];
            String start = request[4];
            String end = request[5];
            String purpose = request[6];
                     
            if (!date.trim().equalsIgnoreCase("today")
                    && !date.trim().equalsIgnoreCase("tmr")) {
                if (date.contains("/")) {
                    userDay = date.split("/")[0].trim();
                    userMonth = date.split("/")[1].trim();
                } else if (date.contains("-")) {
                    userDay = date.split("-")[0].trim();
                    userMonth = date.split("-")[1].trim();
                }
                if (Integer.parseInt(month) == 12 && Integer.parseInt(userMonth) < 12) {
                    year = "" + (Integer.parseInt(year) + 1);
                }
                date = year + "/" + userMonth + "/" + userDay;
            }
        
           
            
            //today and tmr
            if(date.trim().equalsIgnoreCase("today")){
                rs = stmt.executeQuery("SELECT CURDATE() as date");
                while(rs.next()){
                    date = rs.getString("date");
                }
            } else if(date.trim().equalsIgnoreCase("tmr")){
                rs = stmt.executeQuery("SELECT DATE_ADD(CURDATE(), "
                        + "INTERVAL 1 DAY) as date");
                while(rs.next()){
                    date = rs.getString("date");
                }
            }

            if (!isDateValid(date, "yyyy/mm/dd")) {
                sendMessage(channel, "Invalid date!");
                userConfirmation.remove(request);
                return;
            }

            rs = stmt.executeQuery("SELECT FLOOR(TIME_TO_SEC(TIMEDIFF('" + date + " " + start
                    + "', CONVERT_TZ(now(),'+00:00','+07:00')))/60)");
            int timediff = 0;
            
            while(rs.next()){
                timediff = rs.getInt(1);
            }
            if(timediff < -35){
                sendMessage(channel, "It's too late to book this room at the moment.");
                userConfirmation.remove(request);
                return;
            }
            
            rs = stmt.executeQuery("SELECT * from booking WHERE "
                    + "room_id = " + room_id + " "
                    + "AND book_status = 1 "
                    + "AND (("
                    + "end_time > '" + date + " " + start + "'" 
                    + " AND end_time <= '" + date + " " + end + "')" 
                    + "OR (" 
                    + "start_time <= '" + date + " " + start + "'"
                    + "AND start_time > '"+ date + " " + end + "')"
                    + "OR ("
                    + "start_time < '" + date + " " + end + "' "
                    + "AND end_time >= '" + date + " " + end + "'))"
            );           
            
            if(rs.first()){
                sendMessage(channel, "Your time is already chosen, please choose another time!");
                userConfirmation.remove(request);
                return;
            }
            
            stmt.executeUpdate("INSERT INTO booking(request_code, start_time, "
                    + "end_time, booker_id, booker_name, purpose, room_id, book_status, confirmed) VALUES ('"
                    + code + "','"//code
                    + date + " " + start + "','"//start
                    + date + " " + end + "','"//end
                    + booker_id + "','"//booker_id
                    + username + "','" //username
                    + purpose + "',"//purpose
                    + room_id + ","//room_id
                    + "1,"
                    + "0"
                    + ")");

            sendMessage(channel, "Your request is successful!\nBooking code (used for canceling a booking): " + code);
            userConfirmation.remove(request);
            
            stmt.close();
            dbConnection.close();
     
        } catch (SQLException e) {
            sendMessage(channel, "Please check your infomation again!");
        } catch (URISyntaxException ex) {
            Logger.getLogger(MessageManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MessageManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }

    public void cancelRoom(String channel, String[] request){
        try {
            Connection dbConnection = getConnection();
            Statement stmt = dbConnection.createStatement();
            stmt.executeUpdate("UPDATE booking SET book_status = 0"
                    + " WHERE request_code = '" + request[2] + "'"
                    + " AND booker_id = '" + request[1] + "'"
                    + " AND book_status = 1");
            sendMessage(channel, "Your request is successful!");
            userConfirmation.remove(request);
            
            stmt.close();
            dbConnection.close();
        } catch (SQLException e) {
            sendMessage(channel, "Please check your information again!");
            userConfirmation.remove(request);
        } catch (URISyntaxException ex) {
            Logger.getLogger(MessageManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String[] findRequest(String user) {
        for (int i = 0; i < userConfirmation.size(); i++) {
            if (userConfirmation.get(i)[1].equals(user)) {
                return userConfirmation.get(i);
            }
        }
        return null;
    }

    public String generateCode() {
        return "R" + UUID.randomUUID().toString().substring(0, 7).toUpperCase();
    }

    public String jsonToURLEncode(JSONObject json) throws UnsupportedEncodingException {
        String jsonString = json.toString();
        String urlencode = URLEncoder.encode(jsonString, "UTF-8");
        urlencode = urlencode.replace("+", "%20");
        return urlencode;
    }

    public int compareTime(String t1, String t2) {
        String t1a[] = t1.split(":");
        String t2a[] = t2.split(":");

        for (int i = 0; i < t1a.length; i++) {
            if (Integer.parseInt(t1a[i]) > Integer.parseInt(t2a[i])) {
                return 1;
            } else if (Integer.parseInt(t1a[i]) < Integer.parseInt(t2a[i])) {
                return 2;
            }
        }
        return 0;
    }

    public void showBookings(String channel, String user) {
        try {
            //-------HTTP Connection
            connection = (HttpURLConnection) connection.getURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            
            //------- DATABASE CONNECTION
            Connection dbConnection = getConnection();
            
            final DataOutputStream dos = new DataOutputStream(
                    connection.getOutputStream());
            
            //-------GET NUM OF ROOMs
            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(id) FROM room WHERE status = 1");
            int roomCount = 0;
            while (rs.next()) {
                roomCount = rs.getInt(1);
            }
            String[][] room = new String[roomCount][2];
            
            //-------GET ROOM INFO
            rs = stmt.executeQuery("SELECT * FROM room WHERE status = 1");
            int i = 0;
            while (rs.next()) {
                room[i][0] = rs.getString("id");
                room[i][1] = rs.getString("name");
                i++;
            }
          
            
            JSONObject field;
            JSONObject attach;
            JSONArray fieldArray;
            JSONArray atmArray = new JSONArray();
            //-------CREATE ATTACHMENT
            for (int x = 0; x < roomCount; x++) {
                attach = new JSONObject();
                fieldArray = new JSONArray();
                rs = stmt.executeQuery("SELECT * FROM booking"
                        + " WHERE room_id = " + room[x][0] 
                        + " AND book_status = 1"
                        + " AND booker_id = '" + user + "'"
                        + " ORDER BY start_time ASC");
                while (rs.next()) {
                    field = new JSONObject();
                    String date = rs.getString("start_time").split(" ")[0];
                    String start = rs.getString("start_time").split(" ")[1];
                    String end = rs.getString("end_time").split(" ")[1];
                    String purpose = rs.getString("purpose");                  
                    String code = rs.getString("request_code");
                    field.put("value", "[" + date
                            + "]\t" + start.split(":")[0] + ":" + start.split(":")[1]
                            + " - " + end.split(":")[0] + ":" + end.split(":")[1]
                            + "\t\t--->\t\t" + purpose
                            + "\t\t|\t\tCODE: " + code
                    );
                    
                    field.put("short", false);
                    fieldArray.put(field);                 
                }
                
                attach.put("title", room[x][1]);
                attach.put("fields", fieldArray);               
                atmArray.put(attach);
            }
            
            System.out.println(roomCount);
            System.out.println("test: " + atmArray);
            /*for(int x = 0; x < room.length; x++){
            System.out.println("Room Info: " + room[x][0] + " - " + room[x][1]);
            }*/
            
            String attachments = URLEncoder.encode(atmArray.toString(), "UTF-8");
            
            String text = (URLEncoder.encode("*Here's Your Bookings*", "UTF-8"));
            
            String prepare = "token=" + token + "&channel=" + channel + "&text=" + text
                    + "&attachments=" + attachments + "&as_user=true&pretty=1";
            
            dos.writeBytes(prepare);
            dos.flush();
            dos.close();
            stmt.close();
            dbConnection.close();
            final InputStream is = connection.getInputStream();
            is.close();
        } catch (IOException | URISyntaxException | SQLException | JSONException ex) {
            Logger.getLogger(MessageManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static Connection getConnection() throws URISyntaxException, SQLException {
        URI dbUri = new URI(System.getenv("CLEARDB_DATABASE_URL"));

        String username = dbUri.getUserInfo().split(":")[0];
        String password = dbUri.getUserInfo().split(":")[1];
        String dbUrl = "jdbc:mysql://" + dbUri.getHost() + dbUri.getPath();

        return DriverManager.getConnection(dbUrl, username, password);
    }
    
    public String simplifyTime(String time) {   
        try {
            String result;
            time = time.toLowerCase();
            if (time.contains("am")) {
                result = time.replace("am", "");
                String tInHours = timeToHours(result);
                if (Double.parseDouble(tInHours) >= 9
                        && Double.parseDouble(tInHours) <= 11.5) {
                    result = hoursToTime(Double.parseDouble(tInHours));
                } else {
                    return "-1";
                }

            } else if (time.contains("pm")) {
                time = time.replace("pm", "");
                result = time.replace("pm", "");
                String tInHours = timeToHours(result);
                if (Double.parseDouble(tInHours) >= 1 && Double.parseDouble(tInHours) <= 6){
                    result = hoursToTime(Double.parseDouble(tInHours) + 12);
                } else if (Double.parseDouble(tInHours) >= 12 && Double.parseDouble(tInHours) <= 12.5){
                    result = hoursToTime(Double.parseDouble(tInHours));
                } else {
                    return "-1";
                }
            } else {
                String tInHours = timeToHours(time);
                if (Double.parseDouble(tInHours) >= 9 && Double.parseDouble(tInHours) <= 18) {
                    result = hoursToTime(Double.parseDouble(tInHours));
                } else {
                    return "-1";
                }
            }

            return result;
        }catch (NumberFormatException e){
            return "-1";
        }       
    }
    
    public String addTime(String time, String amount){
        double dur = Double.parseDouble(amount);
        double startTime = Double.parseDouble(timeToHours(time));
        
        String result  = ""+ (dur + startTime);
        
        return hoursToTime(Double.parseDouble(result));
    }
     
    public String timeToHours(String time){
        if(time.contains(":")){
            String result;
            result = time.split(":")[0]+"."+Integer.parseInt(time.split(":")[1])/60.0;
            if(result.contains("0.")){
                result = result.replace("0.", "");
            }
            return result;
        }
        else
        {
            return time;
        }
    }
    
    public String hoursToTime(Double hours){
        String h = "" + hours;
        String hour = "" + Integer.parseInt(h.split("\\.")[0]);
        String decimal = "" + (int) (Integer.parseInt(h.split("\\.")[1])*60
                /Math.pow(10, h.split("\\.")[1].length()));
        if(decimal.length() == 1){
            decimal+="0";
        }
        
        return  hour + ":" + decimal;
    }
    
    public void confirmRoom(String user, String channel, String request){
        try {
            
            Connection dbConnection = getConnection();
            Statement stmt = dbConnection.createStatement();
            String[] keywords = request.split(" ");
            String[] params = {"cancel", user, "", "0"}; // {"cancel", user, code, "0"}
            int indxCommand = 0;
            for (int i = 0; i < keywords.length; i++) {
                if (keywords[i].trim().equalsIgnoreCase("cancel")) {
                    indxCommand = i;
                    break;
                }
            }
            
            //retrieve request code
            for (int i = indxCommand + 1; i < keywords.length; i++) {
                if (keywords[i].startsWith("R") && keywords[i].length() == 8) {
                    params[2] = keywords[i];
                    break;
                }
            }
            
            //not enough info
            if (params[2].equals("")) {
                sendMessage(channel, "You entered invalid code! Please check"
                        + " your code using 'show booking' function");
                return;
            }
                    
            
            //send confirmation to user
            ResultSet rs = stmt.executeQuery("SELECT * FROM booking, room WHERE "
                    + "booking.room_id = room.id AND "
                    + "request_code = '" + params[2] + "'"
                    + " AND booker_id = '" + params[1] + "'"
                    + " AND book_status = 1");
            if(!rs.first()){
                sendMessage(channel, "Sorry, I cannot found your code. Please check"
                        + " your code using 'show booking' function");
                stmt.close();
                dbConnection.close();
                return;
            }
            
            
            rs = stmt.executeQuery("SELECT confirmed, FLOOR(TIME_TO_SEC(TIMEDIFF(start_time, CONVERT_TZ(now(),'+00:00','+07:00')))/60) AS time"
                    + " FROM booking"
                    + " WHERE request_code = '" + params[2] + "'");
            int isConfirmed = 0;
            int timediff = 0;
            while(rs.next()){
                 isConfirmed = rs.getInt("confirmed");
                 timediff = rs.getInt("time");
            }
            
            if(isConfirmed == 1){
                sendMessage(channel, "Already confirmed");
                stmt.close();
                dbConnection.close();
                return;
                
            }else if(timediff > 29 || timediff < -5){
                sendMessage(channel, "It's too soon to confirm!\nOnly confirm 30 minutes before your meeting start");
                stmt.close();
                dbConnection.close();
                return;
            }
            
            stmt.executeUpdate("UPDATE booking SET confirmed = 1"
                    + " WHERE request_code = '" + params[2] + "'");
            sendMessage(channel, "Your request is successful!");
            
            stmt.close();
            dbConnection.close();
        } catch (SQLException e) {
            sendMessage(channel, "Please check your information again!");
        } catch (URISyntaxException ex) {
            Logger.getLogger(MessageManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
   public void bookRoomPrepare(String user, String channel, String request) throws IOException {
        if(findRequest(user) == null){
            String[] params = {"book", user, "", "", "", "", "", "0"};//{"book",user,date,room,start,dur,purpose,"0"}
            userConfirmation.add(params);
        }
              
        switch(findRequest(user)[7]){
            case "0":
                if(!request.contains("book room")){
                    sendMessage(channel, "You want to book a room?\nType 'book room' <room_name>");
                    userConfirmation.remove(findRequest(user));
                    return;
                }
                request = request.replaceFirst("book room", "");
                findRequest(user)[2] = request.trim();
                findRequest(user)[7] = "1";
                sendMessage(channel, "Date?");
                break;
            case "1":
                //check date
                if (!request.trim().equalsIgnoreCase("today")
                        && !request.trim().equalsIgnoreCase("tmr")) {
                                       
                    if (request.length() != 5) {
                        sendMessage(channel, "You need to enter date as dd/mm");
                        userConfirmation.remove(findRequest(user));
                        return;
                    }

                    if (request.contains("/") && request.split("/").length != 2) {
                        sendMessage(channel, "You need to enter date as dd/mm");
                        userConfirmation.remove(findRequest(user));
                        return;
                    } else if (request.contains("-") && request.split("-").length != 2) {
                        sendMessage(channel, "You need to enter date as dd/mm");
                        userConfirmation.remove(findRequest(user));
                        return;
                    }
                }
                findRequest(user)[3] = request;
                findRequest(user)[7] = "2";
                sendMessage(channel, "Start time?");
                break;
            case "2":
                //check start time
                request = simplifyTime(request);

                if (request.equals("-1")) {
                    sendMessage(channel, "You can only book from 9AM to 6PM");
                    userConfirmation.remove(findRequest(user));
                    return;
                } else if (Double.parseDouble(timeToHours(request)) % 0.5 != 0) {
                    sendMessage(channel, "Please check your time (e.g 9:00, 9:30, 10:00)");
                    userConfirmation.remove(findRequest(user));
                    return;
                }
                findRequest(user)[4] = request;
                findRequest(user)[7] = "3";
                sendMessage(channel, "Duration?");
                break;
            case "3":
                //check duration
                if (!request.toLowerCase().contains("hour")) {
                    sendMessage(channel, "Please check your duration! (e.g 0,5hour, 1hour)");
                    userConfirmation.remove(findRequest(user));
                    return;
                }
                request = request.split("hour")[0];
                if (Double.parseDouble(request) % 0.5 != 0) {
                    sendMessage(channel, "Please check your duration! (eg. 0,5hour, 1hour)");
                    userConfirmation.remove(findRequest(user));
                    return;
                }
                //change duration into end_time
                request = addTime(findRequest(user)[4], request);
                if (compareTime(request, "18:00") == 1) {
                    sendMessage(channel, "You can only book from 9AM to 6PM");
                    userConfirmation.remove(findRequest(user));
                    return;
                }
                
                findRequest(user)[5] = request;
                findRequest(user)[7] = "4";
                sendMessage(channel, "Purpose?");
                break;
            case "4":              
                findRequest(user)[6] = request;
                findRequest(user)[7] = "5";
                sendMessage(channel, "Booking info:"
                + "\n\t<> Date: " + findRequest(user)[3]
                + "\n\t<> Room: " + findRequest(user)[2]
                + "\n\t<> Start Time: " + findRequest(user)[4]
                + "\n\t<> End Time: " + findRequest(user)[5]
                + "\n\t<> Purpose: " + findRequest(user)[6]
                + "\nIs it correct? (Yes/No)");
                break;
            
            default:
                break;
        }     
    }
   
   public static boolean isDateValid(String dateString, String pattern) {
        try {
            dateString = dateString.replace("-", "/");
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            if (sdf.format(sdf.parse(dateString)).equals(dateString)) {
                return true;
            }
        } catch (ParseException pe) {
        }

        return false;
    }
}
