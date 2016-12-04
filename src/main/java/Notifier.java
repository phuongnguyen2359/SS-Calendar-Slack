
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author USER
 */
public class Notifier implements Runnable {

    final String TOKEN;
    private Thread t;

    public Notifier(String token) {
        this.TOKEN = token;
    }

    @Override
    public void run() {
        while(true){
            try {
                notifyUser();
                Thread.sleep(60000);
            } catch (InterruptedException | MalformedURLException ex) {
                Logger.getLogger(Notifier.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void notifyUser() throws MalformedURLException {
        System.out.println("Note!");
        HttpURLConnection connection = null;
        final URL urlChat = new URL("https://hooks.slack.com/api/chat.postMessage");;

        try {
            //------- DATABASE CONNECTION
            Connection dbConnection = getConnection();
            Statement stmt = dbConnection.createStatement();

            ResultSet rs = stmt.executeQuery("SELECT booking.booker_id"
                    + ", booking.start_time, booking.end_time, booking.purpose, room.name " 
                    +"FROM booking " +"INNER JOIN room ON booking.room_id = room.id " 
                    +"WHERE request_code != 'R0000000' AND FLOOR(TIME_TO_SEC(TIMEDIFF(start_time, CONVERT_TZ(now(),'+00:00','+07:00')))/60) = 29 "
                    +"AND book_status = 1");
            
            while (rs.next()) {
                System.out.println("Something!");
                connection = (HttpURLConnection) urlChat.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(5000);
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);
                final DataOutputStream dos = new DataOutputStream(
                        connection.getOutputStream());

                String booker = rs.getString("booker_id");
                String room = rs.getString("name");
                String purpose = rs.getString("purpose");
                String startTimeH = rs.getString("start_time").split(" ")[1].split(":")[0];
                String startTimeM = rs.getString("start_time").split(" ")[1].split(":")[1];
                String endTimeH = rs.getString("end_time").split(" ")[1].split(":")[0];
                String endTimeM = rs.getString("end_time").split(" ")[1].split(":")[1];
                String text = startTimeH + ":" + startTimeM
                        + "-" + endTimeH + ":" + endTimeM
                        + " : " + purpose + " at room " + room + "\n" 
                        + "Please confirm your booking before " 
                        + startTimeH + ":" + (Integer.parseInt(startTimeM) + 5)
                        + " (type: confirm _<booking code>_)";
                //String text = "Remember";
                text = URLEncoder.encode(text, "UTF-8");
                System.out.println(text);
                String prepare = "token=" + TOKEN + "&channel=" + booker
                        + "&text=" + text + "&as_user=true&pretty=1";
                dos.writeBytes(prepare);
                dos.flush();
                dos.close();
                final InputStream is = connection.getInputStream();

                is.close();
                System.out.println("sent!");
            }
            
            stmt.close();
            dbConnection.close();
          
            //Thread.sleep(60000 * 25);

        } catch (ProtocolException ex) {
            Logger.getLogger(MessageManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MessageManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException | SQLException ex) {
            Logger.getLogger(SSCalendar.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static Connection getConnection() throws URISyntaxException, SQLException {
        URI dbUri = new URI(System.getenv("CLEARDB_DATABASE_URL"));

        String username = dbUri.getUserInfo().split(":")[0];
        String password = dbUri.getUserInfo().split(":")[1];
        String dbUrl = "jdbc:mysql://" + dbUri.getHost() + dbUri.getPath();

        return DriverManager.getConnection(dbUrl, username, password);
    }

    public void start() {
        if (t == null) {
            t = new Thread(this);
            t.start();
        }
    }
}
