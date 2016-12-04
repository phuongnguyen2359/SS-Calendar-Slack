import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.*;

import java.net.URI;
import java.net.URISyntaxException;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import java.net.URL;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.DeploymentException;
import javax.websocket.Session;


public class SSCalendar {
final static CountDownLatch messageLatch = new CountDownLatch(1);
public final static String TOKEN = System.getenv("SLACK_BOT_TOKEN");//"xoxb-62714102963-QvfdRZE082FPqL7vIhGsbmpV";
  public static void main(String[] args) {
    
    if(TOKEN == null){
        System.out.println("No Token found");
        return;
    }
    HttpURLConnection connectionRTM = null;
        HttpURLConnection connectionChat;
        ObjectMapper mapper = new ObjectMapper();
        try {   
            //----- Continuously update DB
            DBChecker checker = new DBChecker();
            checker.start();
            
            Notifier notifier = new Notifier(TOKEN);
            notifier.start();
            
            
            // -----Create connection
            //final URL url = new URL("https://hooks.slack.com/services/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");
            final URL urlRTM = new URL("https://hooks.slack.com/api/rtm.start");
           
            connectionRTM = (HttpURLConnection) urlRTM.openConnection();
            connectionRTM.setRequestMethod("POST");           
            connectionRTM.setConnectTimeout(5000);
            connectionRTM.setUseCaches(false);
            connectionRTM.setDoInput(true);
            connectionRTM.setDoOutput(true);
      
            final URL urlChat = new URL("https://hooks.slack.com/api/chat.postMessage");
            connectionChat = (HttpURLConnection) urlChat.openConnection();
            connectionChat.setRequestMethod("POST");
            connectionChat.setConnectTimeout(5000);
            connectionChat.setUseCaches(false);
            connectionChat.setDoInput(true);
            connectionChat.setDoOutput(true);
                    
            final String payload = "token=" + TOKEN;
            // Send request
            final DataOutputStream wrRTM = new DataOutputStream(
                    connectionRTM.getOutputStream());
         
            
            wrRTM.writeBytes(payload);
            wrRTM.flush();
            wrRTM.close();
                  
            // Get Response
            final InputStream isRTM = connectionRTM.getInputStream();   
            JsonNode response = mapper.readTree(isRTM);
            isRTM.close();
            //System.out.println(node.findPath("wss").asText());
            URI uri = new URI(response.findPath("url").asText());
            String bot_id = response.findParent("self").findPath("id").asText();
            System.out.println("Connecting to " + uri);            
            WebsocketEndpoint clientEndpoint = new WebsocketEndpoint(uri, mapper, 
                    connectionChat,TOKEN, bot_id);      
            while(true)
            {
                ping(mapper, clientEndpoint.session);
                Thread.sleep(10000);
            }
            //messageLatch.await();
           
        } catch (IOException | URISyntaxException | DeploymentException | InterruptedException e) {
            throw new SlackException(e);
        } finally {
            if (connectionRTM != null) {
                connectionRTM.disconnect();
            }
        }

      }
  

  private static Connection getConnection() throws URISyntaxException, SQLException {
        URI dbUri = new URI(System.getenv("CLEARDB_DATABASE_URL"));

        String username = dbUri.getUserInfo().split(":")[0];
        String password = dbUri.getUserInfo().split(":")[1];
        String dbUrl = "jdbc:mysql://" + dbUri.getHost() + dbUri.getPath();

        return DriverManager.getConnection(dbUrl, username, password);
    }
    private static long socketId = 1;

    private static void ping(ObjectMapper mapper, Session session) {
        try {
            ObjectNode pingMessage = mapper.createObjectNode();
            pingMessage.put("id", ++socketId);
            pingMessage.put("type", "ping");
            String pingJson = pingMessage.toString();
            session.getBasicRemote().sendText(pingJson);
            System.out.println("ping : " + pingJson);
        } catch (IOException ex) {
            Logger.getLogger(SSCalendar.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
