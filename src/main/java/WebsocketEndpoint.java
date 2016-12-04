/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author USER
 */
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;

import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import org.json.JSONException;

/**
 * @author Arun Gupta
 */
@ClientEndpoint
public class WebsocketEndpoint {
    Session session = null;
    URI uri = null;
    ObjectMapper mapper;
    HttpURLConnection connection;
    MessageManager msgMng;
    String token;
    String bot_id;
    
    public WebsocketEndpoint(URI uri, ObjectMapper mapper, HttpURLConnection connection,
            String token, String bot_id) throws DeploymentException, IOException{
        this.uri = uri;
        this.mapper = mapper;
        this.connection = connection;
        this.token = token;
        this.bot_id = bot_id;
        ContainerProvider.getWebSocketContainer().connectToServer(this, this.uri);
        
    }
    
    @OnOpen
    public void onOpen(Session session) throws SQLException, URISyntaxException {
        this.session = session;       
        System.out.println("Connected to endpoint: " + session.getBasicRemote());
        msgMng = new MessageManager(connection, session, mapper, token, bot_id);
    }

    @OnMessage
    public void processMessage(String message) throws InterruptedException, URISyntaxException, IOException, SQLException, ProtocolException, JSONException {       
        JsonNode node = mapper.readTree(message);
        System.out.println(node);               
        msgMng.handleMessage(node);     
    }

    @OnError
    public void processError(Throwable t) {
    }
    
    @OnClose
    public void onClose() throws DeploymentException, IOException {
        System.out.println("Ended");
        ContainerProvider.getWebSocketContainer().connectToServer(this, this.uri);
        System.out.println("RestartWS");
    }
     
    
    
}