/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 *
 * @author trainer
 */
public class ChatHandler extends TextWebSocketHandler {

    private final Map<WebSocketSession, String> sessions = Collections.synchronizedMap(new HashMap<>());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session, "anonymus");

        // Send the session ID only to the connected client
        session.sendMessage(new TextMessage("{\"type\": \"sessionId\", \"sessionId\": \"" + session.getId() + "\"}"));

        // Notify others that a new user has joined
        broadcastMessage(session, " New User joined the chat!");
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();

        // Check if message is a name change request
        if (payload.startsWith("@name:")) {
            String oldName = sessions.getOrDefault(session, "anonymus");
            String newName = payload.substring(6).trim(); // Extract the new name
            sessions.put(session, newName); // Update the username
            broadcastMessage(session, "User " + oldName + " changed name to: " + newName);
        } else {
            // Send the chat message with the current username
            String userName = sessions.getOrDefault(session, "anonymus");
            broadcastMessage(session, userName + ": " + payload);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        broadcastMessage(session, session.getId() + ": wurde abgemeldet!");
        sessions.remove(session);
        System.out.println("Session beendet!");

    }

    private void broadcastMessage(WebSocketSession sender, String message) {
        synchronized (sessions) {
            String senderId = sender.getId();
            String senderName = sessions.getOrDefault(sender, senderId);

            // Create a message object
            Map<String, String> messageMap = new HashMap<>();
            messageMap.put("senderId", senderId);
            messageMap.put("senderName", senderName);
            messageMap.put("message", message);

            try {
                String jsonMessage = objectMapper.writeValueAsString(messageMap);
                for (WebSocketSession session : sessions.keySet()) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(jsonMessage));
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(ChatHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
