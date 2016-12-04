

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
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
public class DBChecker implements Runnable{
    private Thread t;
    
    @Override
    public void run() {
        while(true){
            try {
                updateDB();
                Thread.sleep(60000);
            } catch (InterruptedException ex) {
                Logger.getLogger(DBChecker.class.getName()).log(Level.SEVERE, null, ex);
            }
        }       
    }
    
    public void updateDB(){
    try {
        Connection dbConnection = getConnection();
        Statement stmt = dbConnection.createStatement();
               
        stmt.executeUpdate("UPDATE booking SET book_status = 0 WHERE request_code != 'R0000000'"
                + " AND (book_status = 1"
                + " AND end_time < CONVERT_TZ(now(),'+00:00','+07:00')"
                + " OR (FLOOR(TIME_TO_SEC(TIMEDIFF(start_time, CONVERT_TZ(now(),'+00:00','+07:00')))/60) < -5 AND confirmed = 0))");
              
        stmt.close();
        dbConnection.close();
        
        
    } catch (SQLException ex) {
        Logger.getLogger(SSCalendar.class.getName()).log(Level.SEVERE, null, ex);
    } catch (URISyntaxException ex) {
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

    public void start ()
   {
      if (t == null)
      {
         t = new Thread (this);
         t.start ();
      }
   }
        
}
