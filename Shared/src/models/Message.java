package models;

import java.io.Serializable;

/**
 * Message class for client-server communication
 * This file should be IDENTICAL in both Server and Client projects
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String type;
    private String data;
    private long timestamp;
    
    public Message(String type, String data) {
        this.type = type;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getData() {
        return data;
    }
    
    public void setData(String data) {
        this.data = data;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return "Message{type='" + type + "', data='" + data + "', timestamp=" + timestamp + "}";
    }
}

/**
 * Message Types:
 * 
 * CLIENT -> SERVER:
 * - LOGIN: "username:password"
 * - REGISTER: "username:password"
 * - CREATE_ROOM: ""
 * - JOIN_ROOM: "roomId"
 * - LEAVE_ROOM: "roomId"
 * - START_GAME: "roomId"
 * - GAME_SCORE: "roomId:score"
 * - GET_LEADERBOARD: ""
 * - PING: ""
 * 
 * SERVER -> CLIENT:
 * - LOGIN_SUCCESS: "Welcome username!"
 * - LOGIN_FAIL: "error message"
 * - REGISTER_SUCCESS: "success message"
 * - REGISTER_FAIL: "error message"
 * - ROOM_CREATED: "roomId:playerCount"
 * - ROOM_JOINED: "roomId:playerCount"
 * - JOIN_FAIL: "error message"
 * - PLAYER_JOINED: "username:playerCount"
 * - PLAYER_LEFT: "username:playerCount"
 * - GAME_START: ""
 * - SCORE_UPDATE: "username:score"
 * - LEADERBOARD: "rank.username:score\n..."
 * - ERROR: "error message"
 * - PONG: ""
 */