package server;

import database.DatabaseManager;
import models.Message;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Handles individual client connections
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final DatabaseManager database;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;
    private volatile boolean running = true;
    
    public ClientHandler(Socket socket, DatabaseManager database) {
        this.socket = socket;
        this.database = database;
    }
    
    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            while (running) {
                Message message = (Message) in.readObject();
                handleMessage(message);
            }
        } catch (EOFException e) {
            // Client disconnected normally
            System.err.println("⚠️ Error handling client: ");
        } catch (Exception e) {
            System.err.println("⚠️ Error handling client: " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    
    /**
     * Route messages to appropriate handlers
     */
    private void handleMessage(Message msg) {
        try {
            switch (msg.getType()) {
                case "LOGIN":
                    handleLogin(msg);
                    break;
                case "REGISTER":
                    handleRegister(msg);
                    break;
                case "CREATE_ROOM":
                    handleCreateRoom(msg);
                    break;
                case "JOIN_ROOM":
                    handleJoinRoom(msg);
                    break;
                case "LEAVE_ROOM":
                    handleLeaveRoom(msg);
                    break;
                case "START_GAME":
                    handleStartGame(msg);
                    break;
                case "GAME_SCORE":
                    handleGameScore(msg);
                    break;
                case "GET_LEADERBOARD":
                    handleGetLeaderboard();
                    break;
                case "PING":
                    sendMessage(new Message("PONG", ""));
                    break;
                default:
                    System.out.println("⚠️ Unknown message type: " + msg.getType());
            }
        } catch (Exception e) {
            System.err.println("❌ Error handling message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleLogin(Message msg) {
        String[] data = msg.getData().split(":", 2);
        if (data.length != 2) {
            sendMessage(new Message("LOGIN_FAIL", "Invalid login format"));
            return;
        }
        
        String user = data[0].trim();
        String pass = data[1];
        
        if (database.validateUser(user, pass)) {
            username = user;
            GameServer.registerClient(username, this);
            sendMessage(new Message("LOGIN_SUCCESS", "Welcome " + username + "!"));
            System.out.println("✅ Login successful: " + username);
        } else {
            sendMessage(new Message("LOGIN_FAIL", "Invalid username or password"));
            System.out.println("❌ Login failed for: " + user);
        }
    }
    
    private void handleRegister(Message msg) {
        String[] data = msg.getData().split(":", 2);
        if (data.length != 2) {
            sendMessage(new Message("REGISTER_FAIL", "Invalid registration format"));
            return;
        }
        
        String user = data[0].trim();
        String pass = data[1];
        
        if (user.length() < 3) {
            sendMessage(new Message("REGISTER_FAIL", "Username must be at least 3 characters"));
            return;
        }
        
        if (pass.length() < 4) {
            sendMessage(new Message("REGISTER_FAIL", "Password must be at least 4 characters"));
            return;
        }
        
        if (database.registerUser(user, pass)) {
            sendMessage(new Message("REGISTER_SUCCESS", "Account created! Please login."));
            System.out.println("✅ New user registered: " + user);
        } else {
            sendMessage(new Message("REGISTER_FAIL", "Username already exists"));
            System.out.println("❌ Registration failed (duplicate): " + user);
        }
    }
    
    private void handleCreateRoom(Message msg) {
        if (username == null) {
            sendMessage(new Message("ERROR", "Not logged in"));
            return;
        }
        
        String roomId = "ROOM" + System.currentTimeMillis();
        GameRoom room = GameServer.createRoom(roomId, username);
        room.addPlayer(username);
        
        sendMessage(new Message("ROOM_CREATED", roomId + ":" + room.getPlayerCount()));
    }
    
    private void handleJoinRoom(Message msg) {
        if (username == null) {
            sendMessage(new Message("ERROR", "Not logged in"));
            return;
        }
        
        String roomId = msg.getData().trim();
        GameRoom room = GameServer.getRoom(roomId);
        
        if (room == null) {
            sendMessage(new Message("JOIN_FAIL", "Room not found"));
            return;
        }
        
        if (room.addPlayer(username)) {
            sendMessage(new Message("ROOM_JOINED", roomId + ":" + room.getPlayerCount()));
            
            // Notify all players in room
            GameServer.broadcastToRoom(roomId, 
                new Message("PLAYER_JOINED", username + ":" + room.getPlayerCount()));
            
            System.out.println("👥 " + username + " joined room " + roomId + 
                             " (" + room.getPlayerCount() + "/4)");
        } else {
            sendMessage(new Message("JOIN_FAIL", "Room is full or you're already in it"));
        }
    }
    
    private void handleLeaveRoom(Message msg) {
        String roomId = msg.getData();
        GameRoom room = GameServer.getRoom(roomId);
        
        if (room != null) {
            room.removePlayer(username);
            GameServer.broadcastToRoom(roomId, 
                new Message("PLAYER_LEFT", username + ":" + room.getPlayerCount()));
        }
    }
    
    private void handleStartGame(Message msg) {
        String roomId = msg.getData();
        GameRoom room = GameServer.getRoom(roomId);
        
        if (room != null && room.canStart()) {
            GameServer.broadcastToRoom(roomId, new Message("GAME_START", ""));
            System.out.println("🎮 Game started in room: " + roomId);
        } else {
            sendMessage(new Message("ERROR", "Cannot start game (need 2+ players)"));
        }
    }
    
    private void handleGameScore(Message msg) {
        if (username == null) return;
        
        String[] data = msg.getData().split(":", 2);
        if (data.length != 2) return;
        
        String roomId = data[0];
        int score;
        
        try {
            score = Integer.parseInt(data[1]);
        } catch (NumberFormatException e) {
            return;
        }
        
        // Save to database
        database.saveScore(username, score);
        
        // Update room if multiplayer
        if (!roomId.equals("SINGLE")) {
            GameRoom room = GameServer.getRoom(roomId);
            if (room != null) {
                room.updateScore(username, score);
                GameServer.broadcastToRoom(roomId, 
                    new Message("SCORE_UPDATE", username + ":" + score));
            }
        }
        
        System.out.println("📊 Score saved: " + username + " = " + score);
    }
    
    private void handleGetLeaderboard() {
        String leaderboard = database.getLeaderboard(10);
        sendMessage(new Message("LEADERBOARD", leaderboard));
    }
    
    /**
     * Send message to this client
     */
    public void sendMessage(Message msg) {
        try {
            synchronized (out) {
                out.writeObject(msg);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to send message to " + username);
        }
    }
    
    /**
     * Cleanup on disconnect
     */
    private void cleanup() {
        running = false;
        
        if (username != null) {
            GameServer.unregisterClient(username);
        }
        
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            socket.close();
        } catch (IOException e) {
            // Ignore
        }
    }
    
    public String getUsername() {
        return username;
    }
}